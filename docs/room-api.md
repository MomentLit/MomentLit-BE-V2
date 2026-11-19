# 3D Room API

Phase 1 supports 360-degree panoramas (`PANORAMA`), one room per space, and one private furniture layout per user and space. Floor-plan import is not supported yet. Successful requests return `{ "message": "...", "data": ... }`.

| Method and path | Access | Result |
| --- | --- | --- |
| `POST /rooms/layout-jobs` | Signed in; no space ID needed | `202` with `job_id`, `status`, `result`, `error` |
| `GET /rooms/layout-jobs/{job-id}` | Job creator | `200` with job state; `404` if expired, missing, or owned by another user |
| `GET /spaces/{space-id}/room` | Public | `200` with saved room; `404` when there is no room |
| `PUT /spaces/{space-id}/room` | Space owner or admin | `200` with created or replaced room |
| `DELETE /spaces/{space-id}/room` | Space owner or admin | `200`; saved furniture layouts are retained |
| `GET /spaces/{space-id}/room/my-layout` | Signed in | `200` with the caller's saved layout, or `data: null` |
| `PUT /spaces/{space-id}/room/my-layout` | Signed in; room must exist | `200` with the caller's saved layout |
| `DELETE /spaces/{space-id}/room/my-layout` | Signed in | `200`; no-op if the caller has no saved layout |

## Create a room during space registration

1. Upload a 2:1 image via `POST /images/panorama/upload`, or stitch photos via `POST /images/panorama/stitch`. For automatic detection, send the returned URL in `POST /rooms/layout-jobs`: `{ "image_url": "...", "align": true, "cuboid": false }`. `align` and `cuboid` are optional and default to the values shown. A full queue returns `503`; retry later or switch to manual editing.
2. Poll the job with a bounded timeout. It starts `QUEUED`, can move to `RUNNING`, and ends `SUCCEEDED` or `FAILED`. On success, `result` has `image_url`, `uv`, `z0`, `z1`, and `aligned`. Use that image URL with the returned UVs, which are normalized `[u, v]` points alternating ceiling and floor. `z0` and `z1` are in the same relative unit, and `z1` is negative. With known ceiling height, camera height is `ceiling_height * (-z1) / (z0 - z1)`. On failure, show `error` and allow manual editing. Redis job state expires after 30 minutes from its last update; in-flight jobs do not resume after a worker restart.
3. Once the user confirms geometry and the space has an ID, save it with `PUT /spaces/{space-id}/room`. The endpoint also accepts manually edited geometry without an inference job. Use the original image URL when working against the original panorama. If supplied, `scene_id` must belong to the same space.

```json
{
  "source": "PANORAMA",
  "image_url": "https://momentlit-images.s3.ap-northeast-2.amazonaws.com/images/panorama.jpg",
  "ceiling_height": 2.4,
  "camera_height": 1.5,
  "geometry": {
    "outer": [[-2, -1.5], [2, -1.5], [2, 1.5], [-2, 1.5]],
    "inner": [],
    "camera": [0, 0],
    "yaw": 0
  }
}
```

Coordinates are `[x, z]` in meters. List outer vertices in wall order without repeating the first point. `inner` is a list of wall segments with two endpoints each. `yaw` is in radians, and optional `geometry.corners` stores normalized UV points for re-editing. The response also includes `space_id`, `area_m2`, `area_pyeong`, and `updated_at`.

## Save furniture

Send `{ "items": [{ "type": "sofa-3", "x": 0, "z": 0, "rotation": 0 }] }` to `PUT /spaces/{space-id}/room/my-layout`. Each save replaces the list; `items: []` saves an empty layout. Positions are in meters, rotation is in radians, and the frontend owns the catalog IDs and placement rules.

## Runtime configuration

V25 creates the room and furniture layout tables. Automatic detection needs Redis, S3 read/write access to the image bucket, and a running RoomScan service. Set `LAYOUT_BASE_URL` to its internal URL and `LAYOUT_INTERNAL_API_KEY` to the same value as RoomScan's `INTERNAL_API_KEY`. Keep the key on the server; the browser only calls the backend.
