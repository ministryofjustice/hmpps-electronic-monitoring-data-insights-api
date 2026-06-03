-- Version 6 - March 2026
-- Full month: 2026-03-01 00:00 to 2026-03-31 23:59
-- 1 GPS ping per minute
-- Usually returns to same overnight point
-- Revisits same London locations on multiple days
-- Route rotation offset for this month: 2
-- Non-uniform walking movement with sightseeing / coffee stops

INSERT INTO "position" (
    client_id, device_id, location_id, person_id,
    position_circulation_id, position_direction, position_geometry,
    position_gps_date, position_hdop, position_id,
    position_latitude, position_lbs, position_longitude, position_precision,
    position_recorded_date, position_satellite, position_speed,
    position_uploaded_date, _file_name, _feed_type, _delivery_date,
    _dlt_load_id, _dlt_id, _source_bucket, _source_key, _source_etag,
    __datetime_added
)
WITH days AS (
    SELECT
        d AS day_date,
        date_diff('day', DATE '2026-03-01', d) + 1 AS day_no
    FROM UNNEST(
        SEQUENCE(DATE '2026-03-01', DATE '2026-03-31', INTERVAL '1' DAY)
    ) AS x(d)
),
route_for_day AS (
    SELECT
        day_date,
        day_no,
        1 + CAST(((day_no + 2 - 1) % 7) AS integer) AS route_id
    FROM days
),
route_points AS (
    SELECT *
    FROM (
        VALUES
            -- route_id, sequence, minute_of_day, lat, lon
            -- repeated home overnight: Westminster-ish
            -- home = 51.500729, -0.124625

            -- Route 1: Westminster / South Bank
            (1, 1,   0, 51.500729, -0.124625),
            (1, 2, 487, 51.500729, -0.124625),
            (1, 3, 551, 51.503900, -0.119900),
            (1, 4, 579, 51.503900, -0.119900),
            (1, 5, 642, 51.507400, -0.127800),
            (1, 6, 681, 51.507400, -0.127800),
            (1, 7, 764, 51.511700, -0.124000),
            (1, 8, 814, 51.511700, -0.124000),
            (1, 9, 913, 51.505500, -0.117700),
            (1,10, 946, 51.505500, -0.117700),
            (1,11,1088, 51.500729, -0.124625),
            (1,12,1439, 51.500729, -0.124625),

            -- Route 2: City / Tower Bridge
            (2, 1,   0, 51.500729, -0.124625),
            (2, 2, 492, 51.500729, -0.124625),
            (2, 3, 571, 51.513800, -0.098400),
            (2, 4, 612, 51.513800, -0.098400),
            (2, 5, 701, 51.505500, -0.075400),
            (2, 6, 755, 51.505500, -0.075400),
            (2, 7, 844, 51.500900, -0.081200),
            (2, 8, 878, 51.500900, -0.081200),
            (2, 9, 998, 51.507900, -0.098600),
            (2,10,1032, 51.507900, -0.098600),
            (2,11,1098, 51.500729, -0.124625),
            (2,12,1439, 51.500729, -0.124625),

            -- Route 3: Shoreditch / Spitalfields
            (3, 1,   0, 51.500729, -0.124625),
            (3, 2, 481, 51.500729, -0.124625),
            (3, 3, 574, 51.518600, -0.081300),
            (3, 4, 610, 51.518600, -0.081300),
            (3, 5, 682, 51.524500, -0.077500),
            (3, 6, 721, 51.524500, -0.077500),
            (3, 7, 809, 51.521900, -0.071300),
            (3, 8, 862, 51.521900, -0.071300),
            (3, 9, 957, 51.525500, -0.083700),
            (3,10, 993, 51.525500, -0.083700),
            (3,11,1105, 51.500729, -0.124625),
            (3,12,1439, 51.500729, -0.124625),

            -- Route 4: Camden / Regent's Park
            (4, 1,   0, 51.500729, -0.124625),
            (4, 2, 489, 51.500729, -0.124625),
            (4, 3, 585, 51.520000, -0.134000),
            (4, 4, 622, 51.520000, -0.134000),
            (4, 5, 704, 51.529800, -0.153700),
            (4, 6, 759, 51.529800, -0.153700),
            (4, 7, 842, 51.535300, -0.145600),
            (4, 8, 883, 51.535300, -0.145600),
            (4, 9, 972, 51.523800, -0.158600),
            (4,10,1007, 51.523800, -0.158600),
            (4,11,1103, 51.500729, -0.124625),
            (4,12,1439, 51.500729, -0.124625),

            -- Route 5: Hyde Park / Kensington
            (5, 1,   0, 51.500729, -0.124625),
            (5, 2, 496, 51.500729, -0.124625),
            (5, 3, 579, 51.501400, -0.141900),
            (5, 4, 616, 51.501400, -0.141900),
            (5, 5, 715, 51.507300, -0.165700),
            (5, 6, 768, 51.507300, -0.165700),
            (5, 7, 851, 51.502000, -0.174600),
            (5, 8, 899, 51.502000, -0.174600),
            (5, 9,1006, 51.499500, -0.192000),
            (5,10,1037, 51.499500, -0.192000),
            (5,11,1115, 51.500729, -0.124625),
            (5,12,1439, 51.500729, -0.124625),

            -- Route 6: Notting Hill / Paddington
            (6, 1,   0, 51.500729, -0.124625),
            (6, 2, 484, 51.500729, -0.124625),
            (6, 3, 589, 51.515400, -0.175900),
            (6, 4, 626, 51.515400, -0.175900),
            (6, 5, 709, 51.513600, -0.201600),
            (6, 6, 746, 51.513600, -0.201600),
            (6, 7, 829, 51.511900, -0.205900),
            (6, 8, 872, 51.511900, -0.205900),
            (6, 9, 972, 51.517300, -0.178700),
            (6,10,1004, 51.517300, -0.178700),
            (6,11,1110, 51.500729, -0.124625),
            (6,12,1439, 51.500729, -0.124625),

            -- Route 7: Soho / Covent Garden
            (7, 1,   0, 51.500729, -0.124625),
            (7, 2, 491, 51.500729, -0.124625),
            (7, 3, 566, 51.507400, -0.127800),
            (7, 4, 604, 51.507400, -0.127800),
            (7, 5, 687, 51.513600, -0.136200),
            (7, 6, 742, 51.513600, -0.136200),
            (7, 7, 831, 51.515900, -0.143700),
            (7, 8, 879, 51.515900, -0.143700),
            (7, 9, 969, 51.511700, -0.124000),
            (7,10,1002, 51.511700, -0.124000),
            (7,11,1087, 51.500729, -0.124625),
            (7,12,1439, 51.500729, -0.124625)
    ) AS t(route_id, seq_no, minute_of_day, lat, lon)
),
waypoints AS (
    SELECT
        r.day_no,
        date_add('minute', rp.minute_of_day, CAST(r.day_date AS timestamp)) AS ts,
        rp.lat,
        rp.lon
    FROM route_for_day r
    JOIN route_points rp
        ON r.route_id = rp.route_id
),
segments AS (
    SELECT
        day_no,
        ts AS start_ts,
        LEAD(ts) OVER (PARTITION BY day_no ORDER BY ts) AS end_ts,
        lat AS start_lat,
        lon AS start_lon,
        LEAD(lat) OVER (PARTITION BY day_no ORDER BY ts) AS end_lat,
        LEAD(lon) OVER (PARTITION BY day_no ORDER BY ts) AS end_lon
    FROM waypoints
),
pings AS (
    SELECT t AS ping_ts
    FROM UNNEST(
        SEQUENCE(
            TIMESTAMP '2026-03-01 00:00:00',
            TIMESTAMP '2026-03-31 23:59:00',
            INTERVAL '1' MINUTE
        )
    ) AS x(t)
),
positions AS (
    SELECT
        p.ping_ts,
        s.day_no,
        s.start_ts,
        s.end_ts,
        s.start_lat,
        s.start_lon,
        s.end_lat,
        s.end_lon,
        ROW_NUMBER() OVER (ORDER BY p.ping_ts) AS global_rn,
        date_diff('minute', CAST(date(p.ping_ts) AS timestamp), p.ping_ts) + 1 AS rn_day,
        CAST(date_diff('second', s.start_ts, p.ping_ts) AS double)
            / NULLIF(CAST(date_diff('second', s.start_ts, s.end_ts) AS double), 0) AS progress
    FROM pings p
    JOIN segments s
        ON p.ping_ts >= s.start_ts
       AND p.ping_ts < s.end_ts
    WHERE s.end_ts IS NOT NULL
),
realistic_positions AS (
    SELECT
        ping_ts,
        day_no,
        rn_day,
        global_rn,

        CASE
            WHEN start_lat = end_lat AND start_lon = end_lon THEN
                start_lat + (sin(global_rn * 0.37) * 0.000012)
            ELSE
                start_lat
                + ((end_lat - start_lat) *
                    greatest(
                        0.0,
                        least(
                            1.0,
                            progress
                            + (sin(progress * pi() * 2) * 0.085)
                            + (sin(progress * pi() * 5) * 0.045)
                            + (sin(global_rn * 0.017) * 0.018)
                        )
                    )
                )
                + (sin(progress * pi()) * sin(global_rn * 0.19) * 0.00065)
                + (sin(progress * pi() * 3) * 0.00025)
        END AS lat,

        CASE
            WHEN start_lat = end_lat AND start_lon = end_lon THEN
                start_lon + (cos(global_rn * 0.41) * 0.000012)
            ELSE
                start_lon
                + ((end_lon - start_lon) *
                    greatest(
                        0.0,
                        least(
                            1.0,
                            progress
                            + (sin(progress * pi() * 2) * 0.085)
                            + (sin(progress * pi() * 5) * 0.045)
                            + (cos(global_rn * 0.021) * 0.018)
                        )
                    )
                )
                + (sin(progress * pi()) * cos(global_rn * 0.23) * 0.00075)
                + (cos(progress * pi() * 3) * sin(progress * pi()) * 0.00028)
        END AS lon
    FROM positions
)
SELECT
    42,
    1782,
    72,
    CAST(10000 AS bigint),
    CAST(NULL AS bigint),
    0,
    format(
        '{"type":"Point","crs":{"type":"name","properties":{"name":"EPSG:4326"}},"coordinates":[%s,%s]}',
        CAST(lon AS varchar),
        CAST(lat AS varchar)
    ),
    ping_ts,
    16,
    63000000 + global_rn,
    lat,
    5,
    lon,
    CASE
        WHEN rn_day = 877 THEN 900 + CAST(day_no AS integer)
        WHEN rand() < 0.95 THEN CAST(floor(rand() * 3) AS integer)
        ELSE CAST(3 + floor(rand() * 3) AS integer)
    END,
    ping_ts + INTERVAL '6' MINUTE,
    0,
    0,
    ping_ts + INTERVAL '6' MINUTE,
    format(
        'position_london_walk_%s_v6_mar.jsonl',
        replace(CAST(date(ping_ts) AS varchar), '-', '')
    ),
    'specials',
    CAST(date(ping_ts) AS date),
    CAST(NULL AS varchar),
    CAST(NULL AS varchar),
    'emds-dev-raw-formatted-data-20241121110405046600000001',
    format(
        'allied/mdss/position/%s/position_london_walk_%s_v6_mar.jsonl',
        CAST(date(ping_ts) AS varchar),
        replace(CAST(date(ping_ts) AS varchar), '-', '')
    ),
    format('synthetic-london-walk-v6-mar-%s', CAST(date(ping_ts) AS varchar)),
    CAST(current_timestamp AS timestamp(6) with time zone)
FROM realistic_positions;