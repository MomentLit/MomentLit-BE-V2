#!/usr/bin/env python3
"""Real HTTP verification against a fresh, isolated PostgreSQL Compose project.

Requires Python 3.9+, Docker Compose, and a freshly built bootJar.
No production credentials or database are read. Leaves its own stack for inspection.
Only the admin account role is bootstrapped through SQL; all business data use APIs.
"""
import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import sys
import time
import urllib.error
import urllib.request
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--port', type=int, default=18080)
parser.add_argument('--runtime-image', default='momentlit-be-v2-app:latest')
args = parser.parse_args()
now = dt.datetime.now(ZoneInfo('Asia/Seoul'))
run_id = now.strftime('%Y%m%d-%H%M%S') + '-' + secrets.token_hex(2)
project = 'v2-dateverify-' + run_id
out = ROOT / 'docs/verification/date-search' / run_id
out.mkdir(parents=True)
jars = list((ROOT / 'build/libs').glob('*.jar'))
jars = [p for p in jars if not p.name.endswith('-plain.jar')]
if len(jars) != 1:
    sys.exit('Build exactly one bootJar with ./gradlew test bootJar --no-daemon first.')
jar = jars[0]
env = dict(os.environ)
env.update(DATE_SEARCH_DB_PASSWORD=secrets.token_urlsafe(32),
           DATE_SEARCH_JWT_SECRET=secrets.token_urlsafe(64),
           DATE_SEARCH_JAR=str(jar), DATE_SEARCH_API_PORT=str(args.port),
           DATE_SEARCH_RUNTIME_IMAGE=args.runtime_image)
compose = ['docker', 'compose', '-p', project, '-f',
           str(ROOT / 'scripts/date-search-verification/compose.yaml')]
base = f'http://127.0.0.1:{args.port}'
evidence = {'executed_at_seoul': now.isoformat(), 'project': project,
            'base_url': base, 'git_head': subprocess.check_output(
                ['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
            'jar_sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
            'requests': [], 'scenarios': []}
today = now.date()
d1 = today + dt.timedelta(days=((0 - today.weekday()) % 7 or 7))
d2, d3 = d1 + dt.timedelta(days=1), d1 + dt.timedelta(days=7)
evidence['dates'] = dict(D1=str(d1), D2=str(d2), D3=str(d3))
space_id = None

def cmd(arguments, input_text=None):
    return subprocess.check_output(arguments, input=input_text, text=True,
                                   env=env, cwd=ROOT, stderr=subprocess.STDOUT).strip()

def sql(statement):
    return cmd(compose + ['exec', '-T', 'postgres', 'psql', '-X', '-v',
                         'ON_ERROR_STOP=1', '-U', 'verifier', '-d',
                         'date_search_verification', '-At'], statement)

def redact(value):
    if isinstance(value, dict):
        return {k: ('[REDACTED]' if any(word in k.lower() for word in
                    ('password', 'token', 'secret')) else redact(v))
                for k, v in value.items()}
    if isinstance(value, list):
        return [redact(v) for v in value]
    return value

def request(method, path, body=None, token=None, expected=200):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(base + path,
          data=json.dumps(body).encode() if body is not None else None,
          headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            status, raw = response.status, response.read().decode()
    except urllib.error.HTTPError as error:
        status, raw = error.code, error.read().decode()
    response_body = json.loads(raw) if raw else None
    evidence['requests'].append({'at': dt.datetime.now(ZoneInfo('Asia/Seoul')).isoformat(),
        'method': method, 'path': path, 'actor': 'authenticated' if token else 'anonymous',
        'request_body': redact(body), 'status': status, 'response': redact(response_body)})
    print(f'{method} {path} -> {status}', flush=True)
    assert status == expected, f'{method} {path}: expected {expected}, got {status}: {redact(response_body)}'
    return response_body['data'] if response_body else None

def search(label, date, expected_present, query=None):
    path = '/spaces' + (f'?date={date}' if date else '')
    if query:
        path += ('&' if '?' in path else '?') + query
    try:
        page = request('GET', path)
        ids = [s['space_id'] for s in page['content']]
        actual = space_id in ids
        # There is exactly one space in this fresh database. Check the whole page.
        assert page['page'] == 0
        assert page['totalElements'] == len(ids) == (1 if expected_present else 0)
        assert ids == ([space_id] if expected_present else [])
        passed = actual == expected_present
        row = dict(scenario=label, date=str(date) if date else None, request='GET ' + path,
                   http=200, expected_present=expected_present, actual_present=actual,
                   ids=ids, totalElements=page['totalElements'], result='PASS' if passed else 'FAIL')
        evidence['scenarios'].append(row)
        assert passed, row
    except Exception as error:
        if not evidence['scenarios'] or evidence['scenarios'][-1]['scenario'] != label:
            evidence['scenarios'].append(dict(scenario=label, date=str(date) if date else None,
                request='GET ' + path, result='FAIL', error=str(error)))
        raise

def matching_state(token, matching_id, expected):
    items = request('GET', '/matchings/inbox', token=token)['matchings']
    matching = next(m for m in items if m['matching_id'] == matching_id)
    assert matching['space_id'] == space_id and matching['status'] == expected, matching
    assert matching['start_time'].startswith(str(d1))
    return matching

try:
    print('Isolated project: ' + project, flush=True)
    print(cmd(compose + ['up', '-d', '--wait', '--wait-timeout', '120']), flush=True)
    # An actual search response, rather than /health, establishes API readiness.
    for attempt in range(90):
        try:
            with urllib.request.urlopen(base + '/spaces', timeout=2) as ready:
                if ready.status == 200:
                    break
        except (OSError, urllib.error.URLError):
            time.sleep(2)
    else:
        raise RuntimeError('API did not become ready within 180 seconds')
    assert sql('SELECT count(*) FROM spaces.spaces;') == '0'
    assert sql('SELECT count(*) FROM users.users;') == '0'
    evidence['postgres_version'] = sql('SHOW server_version;')
    evidence['java_version'] = cmd(compose + ['exec', '-T', 'app', 'java', '-version'])
    evidence['flyway'] = json.loads(sql("SELECT coalesce(json_agg(t), '[]') FROM (SELECT version, description, success FROM public.flyway_schema_history ORDER BY installed_rank) t;"))
    password = secrets.token_urlsafe(24)
    accounts = {}
    for idx, role in enumerate(('host', 'guest', 'admin')):
        email = f'{role}@dateverify.example'
        user_id = request('POST', '/users/signup', dict(email=email, password=password,
            name='Date verification ' + role, phone=f'0109000000{idx}'), expected=201)['user_id']
        accounts[role] = dict(email=email, user_id=user_id)
    # No admin creation endpoint exists. Bootstrap only this new test account's role.
    sql("UPDATE users.users SET role = 'ADMIN' WHERE email = 'admin@dateverify.example';")
    for role, account in accounts.items():
        login = request('POST', '/auth/signin', dict(email=account['email'], password=password))
        expected_role = 'ROLE_ADMIN' if role == 'admin' else 'ROLE_USER'
        assert login['role'] == expected_role, f"Unexpected login role: {login['role']}"
        account['token'] = login['access_token']
    host, guest, admin = (accounts[r]['token'] for r in ('host', 'guest', 'admin'))
    space_id = request('POST', '/spaces', dict(name='Date Verification Space A',
        description='Isolated API verification fixture',
        address=dict(sido='서울특별시', sigungu='강남구', road_address='서울특별시 강남구 테스트로 1'),
        price_per_hour=10000, category='MEETING_ROOM', capacity=10, area=30,
        usage_unit='HOURLY', is_draft=False), token=host, expected=201)['space_id']
    evidence['space_id'] = space_id
    assert sql('SELECT count(*) FROM spaces.spaces;') == '1'
    request('PATCH', f'/admin/spaces/{space_id}/approve', token=admin, expected=202)
    public_state = json.loads(sql(f"SELECT row_to_json(t) FROM (SELECT id, admin_status, is_active FROM spaces.spaces WHERE id={space_id}) t;"))
    assert public_state == dict(id=space_id, admin_status='APPROVED', is_active=True)
    evidence['public_state'] = public_state
    slots = [dict(day_of_week=day, start_time='09:00:00', end_time='18:00:00', is_open=is_open)
             for day, is_open in (('MONDAY', True), ('TUESDAY', False))]
    request('PUT', f'/spaces/{space_id}/availability', slots, token=host, expected=204)
    availability = request('GET', f'/spaces/{space_id}/availability')['availabilities']
    assert len(availability) == 2
    assert {(s['day_of_week'], s['is_open']) for s in availability} == {('MONDAY', True), ('TUESDAY', False)}
    assert request('GET', f'/spaces/{space_id}/booked-dates')['dates'] == []
    search('1. no date', None, True)
    search('2. open Monday before reservation', d1, True)
    search('3. closed Tuesday', d2, False)
    matching_id = request('POST', '/matchings', dict(space_id=space_id,
        start_time=f'{d1}T10:00:00+09:00', end_time=f'{d1}T11:00:00+09:00',
        total_price='10000', guest_count=2), token=guest, expected=201)['matching_id']
    evidence['matching_id'] = matching_id
    matching_state(host, matching_id, 'REQUESTED')
    assert request('GET', f'/spaces/{space_id}/booked-dates')['dates'] == []
    search('4a. pending reservation does not exclude', d1, True)
    request('PATCH', f'/matchings/{matching_id}/approve', token=host, expected=204)
    matching_state(host, matching_id, 'APPROVED')
    assert request('GET', f'/spaces/{space_id}/booked-dates')['dates'] == [str(d1)]
    search('4b. Monday after host approval', d1, False)
    search('5. next Monday remains available', d3, True)
    search('extra. no date after approval', None, True)
    # size=1 with one matching row forces Spring Data's count query to run.
    search('extra. no-date pagination count', None, True, 'page=0&size=1')
    search('extra. dated pagination count', d3, True, 'page=0&size=1')
    evidence['db_matching'] = json.loads(sql(f"SELECT row_to_json(t) FROM (SELECT id, space_id, status, start_time, end_time FROM matchings.matchings WHERE id={matching_id}) t;"))
    evidence['db_booked_dates'] = json.loads(sql(f"SELECT coalesce(json_agg(t), '[]') FROM (SELECT space_id, date, matching_id FROM spaces.space_booked_dates WHERE space_id={space_id} ORDER BY date) t;"))
    assert evidence['db_matching']['status'] == 'APPROVED'
    assert evidence['db_booked_dates'] == [dict(space_id=space_id, date=str(d1), matching_id=matching_id)]
    evidence['result'] = 'PASS'
except Exception as error:
    evidence['result'] = 'FAIL'
    evidence['error'] = f'{type(error).__name__}: {error}'
    print('Verification failed: ' + evidence['error'], file=sys.stderr, flush=True)
except KeyboardInterrupt:
    evidence['result'] = 'INTERRUPTED'
    evidence['error'] = 'KeyboardInterrupt: verification interrupted before completion'
finally:
    evidence['finished_at_seoul'] = dt.datetime.now(ZoneInfo('Asia/Seoul')).isoformat()
    try:
        inventory = cmd(compose + ['ps', '-q']).splitlines()
        evidence['containers'] = [json.loads(cmd(['docker', 'inspect', '--format',
            '{{json .Name}}', container])).lstrip('/') for container in inventory]
        logs = cmd(compose + ['logs', '--no-color'])
        # Protect generated secrets even if a future startup diagnostic echoes them.
        for sensitive in (env['DATE_SEARCH_DB_PASSWORD'], env['DATE_SEARCH_JWT_SECRET'],
                          locals().get('password', '')):
            if sensitive:
                logs = logs.replace(sensitive, '[REDACTED]')
        logs = re.sub(r'(Using generated security password:)\s*\S+', r'\1 [REDACTED]', logs)
        (out / 'server.log').write_text(logs + '\n')
    except Exception as log_error:
        evidence['log_capture_error'] = str(log_error)
    (out / 'evidence.json').write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n')
    print('Evidence: ' + str(out / 'evidence.json'), flush=True)
    print('Stack retained; stop only these containers: ' + ' '.join(evidence.get('containers', [])), flush=True)
sys.exit(0 if evidence['result'] == 'PASS' else 1)
