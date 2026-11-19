# 3D Room API

Phase 1 supports 360-degree panoramas (`PANORAMA`). A space can have up to 20 rooms, usually one per 360-degree scene, and each user can keep up to 20 named private furniture layouts per room. Floor-plan import is not supported yet. Successful requests return `{ "message": "...", "data": ... }`.

| Method and path | Access | Result |
| --- | --- | --- |
| `POST /rooms/layout-jobs` | Signed in; no space ID needed | `202` with `job_id`, `status`, `result`, `error` |
| `GET /rooms/layout-jobs/{job-id}` | Job creator | `200` with job state; `404` if expired, missing, or owned by another user |
| `GET /spaces/{space-id}/rooms` | Public | `200` with the space's rooms ordered by `sort_order` (empty list if none) |
| `GET /spaces/{space-id}/rooms/{room-id}` | Public | `200` with the room; `404` if missing or in another space |
| `POST /spaces/{space-id}/rooms` | Space owner or admin | `201` with the new room; `400` when the space already has 20 |
| `PUT /spaces/{space-id}/rooms/{room-id}` | Space owner or admin | `200` with the replaced room |
| `DELETE /spaces/{space-id}/rooms/{room-id}` | Space owner or admin | `200`; also deletes every furniture layout saved for that room |
| `GET /spaces/{space-id}/rooms/{room-id}/layouts` | Signed in | `200` with the caller's layouts for the room, most recently updated first |
| `POST /spaces/{space-id}/rooms/{room-id}/layouts` | Signed in | `201` with the new layout; `400` when the caller already has 20 for this room |
| `PUT /spaces/{space-id}/rooms/{room-id}/layouts/{layout-id}` | Layout owner | `200` with the replaced layout; `404` if missing or another user's |
| `DELETE /spaces/{space-id}/rooms/{room-id}/layouts/{layout-id}` | Layout owner | `200`; `404` if missing or another user's |

## Create a room during space registration

1. Upload a 2:1 image via `POST /images/panorama/upload`, or stitch photos via `POST /images/panorama/stitch`. For automatic detection, send the returned URL in `POST /rooms/layout-jobs`: `{ "image_url": "...", "align": true, "cuboid": false }`. `align` and `cuboid` are optional and default to the values shown. A full queue returns `503`; retry later or switch to manual editing.
2. Poll the job with a bounded timeout. It starts `QUEUED`, can move to `RUNNING`, and ends `SUCCEEDED` or `FAILED`. On success, `result` has `image_url`, `uv`, `z0`, `z1`, and `aligned`. Use that image URL with the returned UVs, which are normalized `[u, v]` points alternating ceiling and floor. `z0` and `z1` are in the same relative unit, and `z1` is negative. With known ceiling height, camera height is `ceiling_height * (-z1) / (z0 - z1)`. On failure, show `error` and allow manual editing. Redis job state expires after 30 minutes from its last update; in-flight jobs do not resume after a worker restart.
3. Once the user confirms geometry and the space has an ID, create it with `POST /spaces/{space-id}/rooms` (one per 360-degree scene), or replace an existing one with `PUT /spaces/{space-id}/rooms/{room-id}`. The endpoints also accept manually edited geometry without an inference job. Use the original image URL when working against the original panorama. `name` is required (1–100 characters); `sort_order` defaults to 0. `scene_image_url` records the 360-degree scene the room was made from. Scene IDs can change when a space is edited, so clients match rooms to scenes by this URL.

```json
{
  "name": "거실",
  "sort_order": 0,
  "scene_image_url": "https://momentlit-images.s3.ap-northeast-2.amazonaws.com/images/scene.jpg",
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

Coordinates are `[x, z]` in meters. List outer vertices in wall order without repeating the first point. `inner` is a list of wall segments with two endpoints each. `yaw` is in radians, and optional `geometry.corners` stores normalized UV points for re-editing. The response also includes `room_id`, `space_id`, `area_m2`, `area_pyeong`, and `updated_at`.

## Save furniture

Send `{ "name": "거실 A안", "items": [{ "type": "sofa-3", "x": 0, "z": 0, "rotation": 0 }] }` to `POST /spaces/{space-id}/rooms/{room-id}/layouts` to save a new layout, or to `PUT /spaces/{space-id}/rooms/{room-id}/layouts/{layout-id}` to overwrite one. `name` is required (1–50 characters); `items: []` saves an empty layout. Responses include `layout_id`, `room_id`, `space_id`, `name`, `items`, and `updated_at`. Positions are in meters, rotation is in radians, and the frontend owns the catalog IDs and placement rules.

## Runtime configuration

V25 creates the room and furniture layout tables. Automatic detection needs Redis, S3 read/write access to the image bucket, and a running RoomScan service. Set `LAYOUT_BASE_URL` to its internal URL and `LAYOUT_INTERNAL_API_KEY` to the same value as RoomScan's `INTERNAL_API_KEY`. Keep the key on the server; the browser only calls the backend.
