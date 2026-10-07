#!/usr/bin/env python3
"""Verify matching conflicts, automatic rejection and alarms via real HTTP/PostgreSQL.

Uses the existing verification Compose template with a fresh project and volume.
Never resets existing databases. Stops only its own containers; retains evidence/data.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--port', type=int, default=18082)
parser.add_argument('--runtime-image', default='momentlit-be-v2-app:latest')
args = parser.parse_args()
now = dt.datetime.now(ZoneInfo('Asia/Seoul'))
run_id = now.strftime('%Y%m%d-%H%M%S') + '-' + secrets.token_hex(2)
project = 'v2-matchingverify-' + run_id
out = ROOT / 'docs/verification/matching' / run_id
out.mkdir(parents=True)
jars = [p for p in (ROOT / 'build/libs').glob('*.jar') if not p.name.endswith('-plain.jar')]
assert len(jars) == 1, 'Build one bootJar before verification'
env = dict(os.environ)
env.update(DATE_SEARCH_DB_PASSWORD=secrets.token_urlsafe(32),
           DATE_SEARCH_JWT_SECRET=secrets.token_urlsafe(64),
           DATE_SEARCH_JAR=str(jars[0]), DATE_SEARCH_API_PORT=str(args.port),
           DATE_SEARCH_RUNTIME_IMAGE=args.runtime_image)
compose = ['docker', 'compose', '-p', project, '-f',
           str(ROOT / 'scripts/date-search-verification/compose.yaml')]
base = f'http://127.0.0.1:{args.port}'
password = secrets.token_urlsafe(24)
evidence = dict(project=project, base_url=base, executed_at=now.isoformat(),
                jar_sha256=hashlib.sha256(jars[0].read_bytes()).hexdigest(),
                requests=[], scenarios=[])
day = now.date() + dt.timedelta(days=((0 - now.weekday()) % 7 or 7))
AUTO_REJECTION = '같은 공간의 겹치는 시간대에 다른 예약이 승인되어 예약 요청이 자동 거절되었습니다.'


def cmd(arguments, input_text=None):
    return subprocess.check_output(arguments, input=input_text, text=True,
                                   env=env, cwd=ROOT, stderr=subprocess.STDOUT).strip()


def sql(statement):
    return cmd(compose + ['exec', '-T', 'postgres', 'psql', '-X', '-v',
                         'ON_ERROR_STOP=1', '-U', 'verifier', '-d',
                         'date_search_verification', '-At'], statement)


def redact(value):
    if isinstance(value, dict):
        return {k: ('[REDACTED]' if any(w in k.lower() for w in
                    ('password', 'token', 'secret')) else redact(v)) for k, v in value.items()}
    if isinstance(value, list):
        return [redact(v) for v in value]
    return value


def request(method, path, body=None, actor=None, expected=200):
    headers = {'Content-Type': 'application/json'}
    if actor:
        headers['Authorization'] = 'Bearer ' + accounts[actor]['token']
    req = urllib.request.Request(base + path, headers=headers, method=method,
          data=json.dumps(body).encode() if body is not None else None)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            status, raw = response.status, response.read().decode()
    except urllib.error.HTTPError as error:
        status, raw = error.code, error.read().decode()
    parsed = json.loads(raw) if raw else None
    evidence['requests'].append(dict(method=method, path=path, actor=actor,
        body=redact(body), status=status, response=redact(parsed)))
    print(f'{method} {path} -> {status}', flush=True)
    if expected is not None:
        assert status == expected, (method, path, status, redact(parsed))
    return status, parsed


def data(method, path, body=None, actor=None, expected=200):
    _, response = request(method, path, body, actor, expected)
    return response['data'] if response else None


def passed(name):
    evidence['scenarios'].append(dict(scenario=name, result='PASS'))
    print('PASS: ' + name, flush=True)


def space():
    sid = data('POST', '/spaces', dict(name='Matching verification space',
        description='Isolated matching/alarms verification',
        address=dict(sido='서울특별시', sigungu='강남구', road_address='서울특별시 강남구 테스트로 1'),
        price_per_hour=10000, category='MEETING_ROOM', capacity=10, area=30,
        usage_unit='HOURLY', is_draft=False), 'host', 201)['space_id']
    data('PATCH', f'/admin/spaces/{sid}/approve', actor='admin', expected=202)
    data('PUT', f'/spaces/{sid}/availability', [dict(day_of_week='MONDAY',
        start_time='09:00:00', end_time='18:00:00', is_open=True)], 'host', 204)
    return sid


def payload(sid, start='10:00', end='12:00'):
    return dict(space_id=sid, start_time=f'{day}T{start}:00+09:00',
                end_time=f'{day}T{end}:00+09:00', total_price='20000', guest_count=2)


def create(actor, sid, start='10:00', end='12:00'):
    return data('POST', '/matchings', payload(sid, start, end), actor, 201)['matching_id']


def states(expected):
    inbox = data('GET', '/matchings/inbox', actor='host')['matchings']
    actual = {m['matching_id']: m['status'] for m in inbox}
    for mid, status in expected.items():
        assert actual[mid] == status, (mid, actual[mid], status)
    db = json.loads(sql('SELECT coalesce(json_object_agg(id, status), \'{}\') FROM matchings.matchings;'))
    for mid, status in expected.items():
        assert db[str(mid)] == status, (mid, db)


def alarm(actor, mid, description):
    items = [a for a in data('GET', '/alarm', actor=actor) if a['matchingId'] == mid]
    assert len(items) == 1 and items[0]['description'] == description
    assert items[0]['isRead'] is False
    uid = accounts[actor]['user_id']
    rows = json.loads(sql(f"SELECT coalesce(json_agg(t), '[]') FROM (SELECT user_id, matching_id, description, is_read FROM alarms.matchings_alarm WHERE user_id='{uid}' AND matching_id={mid}) t;"))
    assert rows == [dict(user_id=uid, matching_id=mid, description=description, is_read=False)]


def parallel(jobs):
    barrier = threading.Barrier(len(jobs))
    def run(job):
        barrier.wait(timeout=10)
        return request(*job, expected=None)
    with ThreadPoolExecutor(max_workers=len(jobs)) as pool:
        return list(pool.map(run, jobs))


accounts = {}
try:
    print('Isolated project: ' + project, flush=True)
    print(cmd(compose + ['up', '-d', '--wait', '--wait-timeout', '120']), flush=True)
    for _ in range(90):
        try:
            with urllib.request.urlopen(base + '/spaces', timeout=2) as response:
                if response.status == 200:
                    break
        except (OSError, urllib.error.URLError):
            time.sleep(2)
    else:
        raise RuntimeError('API did not become ready')
    assert sql('SELECT count(*) FROM users.users;') == '0'
    assert sql('SELECT count(*) FROM spaces.spaces;') == '0'
    evidence['postgres_version'] = sql('SHOW server_version;')
    for idx, role in enumerate(('host', 'a', 'b', 'c', 'd', 'admin')):
        email = f'{role}@matchingverify.example'
        uid = data('POST', '/users/signup', dict(email=email, password=password,
            name='Matching verify ' + role, phone=f'0109100000{idx}'), expected=201)['user_id']
        accounts[role] = dict(email=email, user_id=uid)
    sql("UPDATE users.users SET role='ADMIN' WHERE email='admin@matchingverify.example';")
    for role, account in accounts.items():
        login = data('POST', '/auth/signin', dict(email=account['email'], password=password))
        assert login['role'] == ('ROLE_ADMIN' if role == 'admin' else 'ROLE_USER')
        account['token'] = login['access_token']

    s1, s2 = space(), space()
    winner = create('a', s1)
    before = sql('SELECT count(*) FROM matchings.matchings; SELECT count(*) FROM alarms.matchings_alarm;')
    _, failure = request('POST', '/matchings', payload(s1), 'a', 409)
    assert '이미 승인 대기' in failure['message']
    assert before == sql('SELECT count(*) FROM matchings.matchings; SELECT count(*) FROM alarms.matchings_alarm;')
    passed('identical pending request blocked; no extra matching or alarm')

    old_rejected = create('b', s1)
    data('PATCH', f'/matchings/{old_rejected}/reject', actor='host', expected=204)
    competitor = create('b', s1)
    old_canceled = create('d', s1)
    data('PATCH', f'/matchings/{old_canceled}/cancel', actor='d', expected=204)
    resubmitted = create('d', s1)
    partial = create('c', s1, '11:00', '13:00')
    same_user_partial = create('a', s1, '11:00', '12:00')
    adjacent = create('b', s1, '12:00', '13:00')
    preceding = create('c', s1, '09:00', '10:00')
    separate = create('c', s1, '14:00', '15:00')
    other_space = create('a', s2)
    passed('competing users allowed; rejected/canceled requests can be resubmitted')
    before = sql('SELECT count(*) FROM alarms.matchings_alarm;')
    request('PATCH', f'/matchings/{winner}/approve', actor='b', expected=403)
    states({winner: 'REQUESTED', competitor: 'REQUESTED'})
    assert before == sql('SELECT count(*) FROM alarms.matchings_alarm;')
    passed('unauthorized approval leaves state and alarms unchanged')
    data('PATCH', f'/matchings/{winner}/approve', actor='host', expected=204)
    expected = {winner: 'APPROVED', old_rejected: 'REJECTED', old_canceled: 'CANCELED',
                competitor: 'REJECTED', resubmitted: 'REJECTED', partial: 'REJECTED',
                same_user_partial: 'REJECTED', adjacent: 'REQUESTED', preceding: 'REQUESTED',
                separate: 'REQUESTED', other_space: 'REQUESTED'}
    states(expected)
    for actor, mid in [('b', competitor), ('d', resubmitted), ('c', partial), ('a', same_user_partial)]:
        alarm(actor, mid, AUTO_REJECTION)
    alarm('a', winner, '예약 요청이 승인되었습니다.')
    for actor, mid in [('b', adjacent), ('c', preceding), ('c', separate), ('a', other_space)]:
        assert not [a for a in data('GET', '/alarm', actor=actor) if a['matchingId'] == mid]
    passed('approval rejects exact/partial overlaps with one unread alarm per requester')
    passed('adjacent/non-overlapping/other-space/terminal requests unchanged')
    before = sql('SELECT count(*) FROM matchings.matchings; SELECT count(*) FROM alarms.matchings_alarm;')
    request('PATCH', f'/matchings/{winner}/approve', actor='host', expected=409)
    request('PATCH', f'/matchings/{competitor}/approve', actor='host', expected=409)
    request('POST', '/matchings', payload(s1, '11:30', '12:30'), 'd', 409)
    assert before == sql('SELECT count(*) FROM matchings.matchings; SELECT count(*) FROM alarms.matchings_alarm;')
    passed('repeated approval/rejected approval/new approved-overlap cause no extra rows or alarms')
    data('PATCH', f'/matchings/{adjacent}/approve', actor='host', expected=204)
    data('PATCH', f'/matchings/{preceding}/approve', actor='host', expected=204)
    states({adjacent: 'APPROVED', preceding: 'APPROVED', separate: 'REQUESTED'})
    passed('touching boundaries can still be approved')

    s3 = space()
    outcomes = parallel([('POST', '/matchings', payload(s3), 'a')] * 8)
    assert sorted(status for status, _ in outcomes) == [201] + [409] * 7
    mid = next(body['data']['matching_id'] for status, body in outcomes if status == 201)
    assert sql(f'SELECT count(*) FROM matchings.matchings WHERE space_id={s3};') == '1'
    host_alarms = [a for a in data('GET', '/alarm', actor='host') if a['matchingId'] == mid]
    assert len(host_alarms) == 1
    passed('eight simultaneous duplicate creates on an empty space yield exactly one matching/alarm')

    s4 = space()
    left, right = create('a', s4), create('b', s4)
    outcomes = parallel([('PATCH', f'/matchings/{m}/approve', None, 'host') for m in (left, right)])
    assert sorted(status for status, _ in outcomes) == [204, 409]
    selected = left if outcomes[0][0] == 204 else right
    rejected = right if selected == left else left
    states({selected: 'APPROVED', rejected: 'REJECTED'})
    alarm('a' if selected == left else 'b', selected, '예약 요청이 승인되었습니다.')
    alarm('b' if rejected == right else 'a', rejected, AUTO_REJECTION)
    passed('simultaneous overlapping approvals yield one approved, one rejected, one alarm each')

    for idx in range(3):
        sid = space()
        selected = create('a', sid)
        outcomes = parallel([('PATCH', f'/matchings/{selected}/approve', None, 'host'),
                             ('POST', '/matchings', payload(sid), 'b')])
        assert outcomes[0][0] == 204 and outcomes[1][0] in (201, 409)
        if outcomes[1][0] == 201:
            loser = outcomes[1][1]['data']['matching_id']
            states({selected: 'APPROVED', loser: 'REJECTED'})
            alarm('b', loser, AUTO_REJECTION)
        else:
            assert sql(f'SELECT count(*) FROM matchings.matchings WHERE space_id={sid};') == '1'
    passed('approval/create races leave no overlapping pending request (three runs)')

    sid = space()
    selected, loser = create('a', sid), create('b', sid)
    outcomes = parallel([('PATCH', f'/matchings/{selected}/approve', None, 'host'),
                         ('PATCH', f'/matchings/{loser}/cancel', None, 'b')])
    assert outcomes[0][0] == 204 and outcomes[1][0] in (204, 409)
    states({selected: 'APPROVED', loser: 'CANCELED' if outcomes[1][0] == 204 else 'REJECTED'})
    if outcomes[1][0] == 409:
        alarm('b', loser, AUTO_REJECTION)
    else:
        assert not [a for a in data('GET', '/alarm', actor='b') if a['matchingId'] == loser]
    passed('cancel/automatic-rejection race preserves the first terminal state')

    # Force only a test request's automatic-rejection alarm to fail in this fresh DB.
    # This verifies actual transaction rollback rather than merely annotations.
    sid = space()
    selected, loser = create('a', sid), create('b', sid)
    before = sql('SELECT count(*) FROM alarms.matchings_alarm;')
    sql(f"""CREATE FUNCTION alarms.fail_verification_alarm() RETURNS trigger AS $$
        BEGIN
            IF NEW.matching_id = {loser} THEN
                RAISE EXCEPTION 'verification alarm failure';
            END IF;
            RETURN NEW;
        END;
        $$ LANGUAGE plpgsql;
        CREATE TRIGGER fail_verification_alarm BEFORE INSERT ON alarms.matchings_alarm
        FOR EACH ROW EXECUTE FUNCTION alarms.fail_verification_alarm();""")
    try:
        request('PATCH', f'/matchings/{selected}/approve', actor='host', expected=500)
        states({selected: 'REQUESTED', loser: 'REQUESTED'})
        assert before == sql('SELECT count(*) FROM alarms.matchings_alarm;')
        assert sql(f'SELECT count(*) FROM spaces.space_booked_dates WHERE space_id={sid};') == '0'
    finally:
        sql('DROP TRIGGER fail_verification_alarm ON alarms.matchings_alarm; DROP FUNCTION alarms.fail_verification_alarm();')
    data('PATCH', f'/matchings/{selected}/approve', actor='host', expected=204)
    states({selected: 'APPROVED', loser: 'REJECTED'})
    alarm('a', selected, '예약 요청이 승인되었습니다.')
    alarm('b', loser, AUTO_REJECTION)
    passed('alarm failure rolls back approval/rejection/booked date/all alarms; retry succeeds')
    evidence['db_matchings'] = json.loads(sql("SELECT json_agg(t) FROM (SELECT id, space_id, seller_id, status, start_time, end_time FROM matchings.matchings ORDER BY id) t;"))
    evidence['db_alarms'] = json.loads(sql("SELECT json_agg(t) FROM (SELECT id, user_id, matching_id, description, is_read FROM alarms.matchings_alarm ORDER BY id) t;"))
    evidence['result'] = 'PASS'
except Exception as error:
    evidence['result'] = 'FAIL'
    evidence['error'] = f'{type(error).__name__}: {error}'
    print(evidence['error'], file=sys.stderr, flush=True)
finally:
    evidence['finished_at'] = dt.datetime.now(ZoneInfo('Asia/Seoul')).isoformat()
    try:
        logs = cmd(compose + ['logs', '--no-color'])
        for sensitive in (env['DATE_SEARCH_DB_PASSWORD'], env['DATE_SEARCH_JWT_SECRET'], password):
            logs = logs.replace(sensitive, '[REDACTED]')
        logs = re.sub(r'(Using generated security password:)\s*\S+', r'\1 [REDACTED]', logs)
        (out / 'server.log').write_text(logs + '\n')
        print(cmd(compose + ['stop']), flush=True)
        evidence['own_containers_stopped'] = True
    except Exception as error:
        evidence['cleanup_error'] = str(error)
    (out / 'evidence.json').write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n')
    print('Evidence: ' + str(out / 'evidence.json'), flush=True)
sys.exit(0 if evidence.get('result') == 'PASS' else 1)
