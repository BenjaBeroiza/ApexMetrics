"""
Genera CSVs de telemetría sintéticos pero realistas para ApexMetrics.
Uso: python3 docs/generate_samples.py
Salida: docs/samples/*.csv
"""
import math
import os

SAMPLES_DIR = os.path.join(os.path.dirname(__file__), "samples")


def lerp(a, b, t):
    return a + (b - a) * t


def smooth_clamp(v, lo, hi):
    return max(lo, min(hi, v))


# ---------------------------------------------------------------------------
# Perfil de velocidad genérico para un circuito paramétrico
# Recibe waypoints (dist_from_start, target_speed) y devuelve una función
# que calcula velocidad suavizada a lo largo de la pista.
# ---------------------------------------------------------------------------

def build_speed_profile(waypoints, total_dist, n_points):
    """Interpola waypoints de velocidad y aplica suavizado por promedio deslizante."""
    speeds = []
    for i in range(n_points):
        d = (i / n_points) * total_dist
        # Encuentra los dos waypoints vecinos
        prev = waypoints[-1]
        nxt = waypoints[0]
        for j in range(len(waypoints)):
            if waypoints[j][0] <= d:
                prev = waypoints[j]
            if waypoints[j][0] > d:
                nxt = waypoints[j]
                break
        if prev == nxt:
            speeds.append(prev[1])
        else:
            span = nxt[0] - prev[0]
            if span <= 0:
                span = total_dist  # wrap-around
                frac = ((d - prev[0]) % total_dist) / span
            else:
                frac = (d - prev[0]) / span
            # Ease-in-out
            t = frac * frac * (3 - 2 * frac)
            speeds.append(lerp(prev[1], nxt[1], t))

    # Suavizado gaussiano ligero
    window = 5
    out = []
    for i in range(n_points):
        total = 0.0
        w = 0.0
        for k in range(-window, window + 1):
            idx = (i + k) % n_points
            weight = math.exp(-0.5 * (k / (window / 2)) ** 2)
            total += speeds[idx] * weight
            w += weight
        out.append(total / w)
    return out


def derive_brake_throttle(speeds):
    """Deriva freno y aceleración a partir del perfil de velocidad."""
    n = len(speeds)
    brakes = []
    throttles = []
    for i in range(n):
        prev_v = speeds[(i - 1) % n]
        curr_v = speeds[i]
        delta = curr_v - prev_v
        max_delta = 15.0  # km/h por punto a full aceleración

        if delta < -2.0:
            # Decelerando → freno proporcional
            b = smooth_clamp(-delta / max_delta * 1.2, 0.0, 1.0)
            t = 0.0
        elif delta > 1.0:
            # Acelerando
            b = 0.0
            t = smooth_clamp(delta / max_delta, 0.1, 1.0)
        else:
            # Velocidad estable (curva de velocidad constante)
            b = 0.0
            t = smooth_clamp(curr_v / 280.0, 0.3, 0.8)

        brakes.append(round(b, 3))
        throttles.append(round(t, 3))

    return brakes, throttles


# ---------------------------------------------------------------------------
# 1. demo_iracing.csv  —  iRacing sin GPS, ~300 puntos, 2 vueltas
# ---------------------------------------------------------------------------

def gen_iracing_no_gps():
    total_dist_per_lap = 5800  # metros (circuito genérico)
    n_per_lap = 150
    n_laps = 2

    # Waypoints (dist_en_vuelta, velocidad_objetivo_km/h)
    waypoints = [
        (0, 120),
        (100, 250), (500, 280), (900, 310),   # recta principal
        (1000, 80),                            # curva 1 (frenada fuerte)
        (1200, 180), (1500, 220), (1800, 260), # sector medio
        (1900, 60),                            # chicane
        (2100, 160), (2400, 210), (2700, 240),
        (2800, 90),                            # horquilla
        (3000, 200), (3300, 260), (3600, 290),
        (3700, 70),                            # curva lenta
        (3900, 180), (4200, 230),
        (4300, 110),
        (4500, 200), (4800, 270), (5100, 300),
        (5200, 85),                            # última frenada
        (5500, 200), (5800, 120),
    ]
    # Normalizar distancias al largo total
    waypoints = [(d * total_dist_per_lap / 5800, v) for d, v in waypoints]

    speeds_lap = build_speed_profile(waypoints, total_dist_per_lap, n_per_lap)
    brakes_lap, throttles_lap = derive_brake_throttle(speeds_lap)

    rows = ["Distance,Speed,Brake,Throttle"]
    for lap in range(n_laps):
        for i in range(n_per_lap):
            dist = i * (total_dist_per_lap / n_per_lap)
            rows.append(f"{dist:.1f},{speeds_lap[i]:.1f},{brakes_lap[i]:.3f},{throttles_lap[i]:.3f}")

    return "\n".join(rows)


# ---------------------------------------------------------------------------
# 2. demo_assetto_corsa.csv  —  AC sin GPS, ~300 puntos, 2 vueltas
#    Formato: pos,speedKmh,brake,gas  (pos se resetea en cada vuelta)
# ---------------------------------------------------------------------------

def gen_ac_no_gps():
    n_per_lap = 150
    n_laps = 2

    waypoints = [
        (0, 100), (10, 220), (30, 260),
        (40, 55),                        # frenada fuerte
        (55, 140), (70, 200), (90, 240),
        (100, 40),                       # horquilla
        (115, 150), (130, 210),
        (140, 70),
        (150, 120), (n_per_lap, 100),
    ]
    waypoints = [(d * n_per_lap / n_per_lap, v) for d, v in waypoints]

    speeds_lap = build_speed_profile(waypoints, n_per_lap, n_per_lap)
    brakes_lap, throttles_lap = derive_brake_throttle(speeds_lap)

    rows = ["pos,speedKmh,brake,gas"]
    for _lap in range(n_laps):
        for i in range(n_per_lap):
            rows.append(f"{i},{speeds_lap[i]:.1f},{brakes_lap[i]:.3f},{throttles_lap[i]:.3f}")

    return "\n".join(rows)


# ---------------------------------------------------------------------------
# 3. demo_iracing_gps.csv  —  iRacing GPS, ~400 puntos, 2 vueltas
#    Circuito: Spa-Francorchamps (lat/lon reales, ~7km por vuelta)
# ---------------------------------------------------------------------------

SPA_WAYPOINTS = [
    # (lat, lon, speed_km_h)  — sentido horario
    (50.4372, 5.9713, 120),   # Start/Finish
    (50.4375, 5.9710, 60),    # La Source hairpin entry
    (50.4370, 5.9700, 55),    # La Source apex
    (50.4358, 5.9685, 90),    # descent
    (50.4341, 5.9669, 130),   # Eau Rouge entry
    (50.4338, 5.9660, 110),   # Eau Rouge apex
    (50.4355, 5.9620, 200),   # Raidillon exit / start Kemmel
    (50.4395, 5.9575, 290),   # Kemmel straight
    (50.4440, 5.9530, 310),   # top of Kemmel (full throttle)
    (50.4452, 5.9520, 100),   # Les Combes braking
    (50.4455, 5.9505, 80),    # Les Combes apex
    (50.4445, 5.9480, 170),   # exit Les Combes
    (50.4420, 5.9440, 210),   # Malmedy straight
    (50.4395, 5.9400, 90),    # Rivage entry
    (50.4375, 5.9390, 75),    # Rivage apex
    (50.4350, 5.9415, 180),   # exit Rivage
    (50.4330, 5.9440, 160),   # Pouhon approach
    (50.4315, 5.9450, 200),   # Pouhon fast left
    (50.4300, 5.9470, 220),   # after Pouhon
    (50.4285, 5.9510, 200),   # Fagnes
    (50.4275, 5.9555, 100),   # Stavelot braking
    (50.4270, 5.9565, 90),    # Stavelot apex
    (50.4272, 5.9600, 240),   # Paul Frere straight
    (50.4280, 5.9650, 290),   # Blanchimont (high speed)
    (50.4290, 5.9695, 100),   # Bus Stop braking
    (50.4305, 5.9705, 75),    # Bus Stop chicane
    (50.4325, 5.9712, 130),   # exit chicane
    (50.4355, 5.9714, 150),   # approach finish
    (50.4372, 5.9713, 120),   # Start/Finish (close lap)
]


def interpolate_gps_track(waypoints, n_points_total):
    """Interpola entre waypoints GPS y genera velocidad/freno/aceleración."""
    n_wps = len(waypoints)
    # Calcula longitud total aproximada (en grados → suficiente para proporción)
    seg_lengths = []
    for i in range(n_wps - 1):
        dlat = waypoints[i+1][0] - waypoints[i][0]
        dlon = waypoints[i+1][1] - waypoints[i][1]
        seg_lengths.append(math.sqrt(dlat**2 + dlon**2))
    total_len = sum(seg_lengths)

    # Distribuye puntos proporcional a longitud de cada segmento
    result = []
    for seg_i in range(n_wps - 1):
        seg_pts = max(2, round(n_points_total * seg_lengths[seg_i] / total_len))
        lat0, lon0, v0 = waypoints[seg_i]
        lat1, lon1, v1 = waypoints[seg_i + 1]
        for k in range(seg_pts):
            t = k / seg_pts
            e = t * t * (3 - 2 * t)  # ease-in-out
            result.append((
                lat0 + (lat1 - lat0) * e,
                lon0 + (lon1 - lon0) * e,
                v0 + (v1 - v0) * e,
            ))

    # Recorta o expande al número deseado
    while len(result) < n_points_total:
        result.append(result[-1])
    return result[:n_points_total]


def gen_iracing_gps():
    n_per_lap = 200
    n_laps = 2

    track = interpolate_gps_track(SPA_WAYPOINTS, n_per_lap)
    speeds = [p[2] for p in track]

    # Suavizado
    window = 4
    smoothed = []
    for i in range(n_per_lap):
        s = sum(speeds[(i + k) % n_per_lap] for k in range(-window, window + 1))
        smoothed.append(s / (2 * window + 1))

    brakes, throttles = derive_brake_throttle(smoothed)

    # Distancia acumulada (aproximación simple en metros)
    R = 6371000
    dist = 0.0

    rows = ["Distance,Speed,Brake,Throttle,Lat,Lon"]
    for lap in range(n_laps):
        prev_lat, prev_lon = track[0][0], track[0][1]
        dist_lap = 0.0
        for i in range(n_per_lap):
            lat, lon, _ = track[i]
            dlat = math.radians(lat - prev_lat)
            dlon = math.radians(lon - prev_lon)
            a = math.sin(dlat/2)**2 + math.cos(math.radians(prev_lat)) * math.cos(math.radians(lat)) * math.sin(dlon/2)**2
            dist_lap += R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
            prev_lat, prev_lon = lat, lon
            rows.append(f"{dist_lap:.1f},{smoothed[i]:.1f},{brakes[i]:.3f},{throttles[i]:.3f},{lat:.6f},{lon:.6f}")
        dist = 0.0  # iRacing resetea distancia en cada vuelta

    return "\n".join(rows)


# ---------------------------------------------------------------------------
# 4. demo_assetto_corsa_pos.csv  —  AC con posición local, ~400 puntos, 2 vueltas
#    Formato: pos,speedKmh,brake,gas,posX,posZ
#    Circuito cerrado con 4 curvas principales, coordenadas 0-800m
# ---------------------------------------------------------------------------

AC_TRACK_WAYPOINTS = [
    # (posX, posZ, speed)  — circuito cerrado
    (400, 50,   120),   # Start/Finish (mitad inferior)
    (600, 50,   280),   # recta principal
    (750, 80,   310),   # fin de recta
    (780, 150,  90),    # curva 1 entrada (lenta)
    (780, 250,  70),    # curva 1 ápex
    (760, 320,  180),   # salida curva 1
    (700, 400,  240),   # sector técnico
    (640, 430,  180),   # curva 2 entrada
    (580, 450,  80),    # curva 2 ápex (horquilla)
    (520, 440,  160),   # salida curva 2
    (450, 420,  200),   # recta posterior
    (350, 430,  220),
    (280, 410,  230),
    (220, 380,  100),   # curva 3 (chicane)
    (190, 350,  80),
    (200, 300,  160),
    (230, 260,  200),
    (200, 200,  240),
    (180, 150,  90),    # curva 4 (última antes de recta)
    (200, 100,  80),
    (250, 70,   160),
    (320, 52,   220),
    (400, 50,   120),   # cierre del circuito
]


def interpolate_ac_track(waypoints, n_points):
    n_wps = len(waypoints)
    seg_lengths = []
    for i in range(n_wps - 1):
        dx = waypoints[i+1][0] - waypoints[i][0]
        dz = waypoints[i+1][1] - waypoints[i][1]
        seg_lengths.append(math.sqrt(dx**2 + dz**2))
    total_len = sum(seg_lengths)

    result = []
    for seg_i in range(n_wps - 1):
        seg_pts = max(2, round(n_points * seg_lengths[seg_i] / total_len))
        x0, z0, v0 = waypoints[seg_i]
        x1, z1, v1 = waypoints[seg_i + 1]
        for k in range(seg_pts):
            t = k / seg_pts
            e = t * t * (3 - 2 * t)
            result.append((x0 + (x1 - x0) * e, z0 + (z1 - z0) * e, v0 + (v1 - v0) * e))

    while len(result) < n_points:
        result.append(result[-1])
    return result[:n_points]


def gen_ac_gps():
    n_per_lap = 200
    n_laps = 2

    track = interpolate_ac_track(AC_TRACK_WAYPOINTS, n_per_lap)
    speeds_raw = [p[2] for p in track]

    # Suavizado
    window = 4
    speeds = []
    for i in range(n_per_lap):
        s = sum(speeds_raw[(i + k) % n_per_lap] for k in range(-window, window + 1))
        speeds.append(s / (2 * window + 1))

    brakes, throttles = derive_brake_throttle(speeds)

    rows = ["pos,speedKmh,brake,gas,posX,posZ"]
    for _lap in range(n_laps):
        for i in range(n_per_lap):
            x, z, _ = track[i]
            rows.append(f"{i},{speeds[i]:.1f},{brakes[i]:.3f},{throttles[i]:.3f},{x:.1f},{z:.1f}")

    return "\n".join(rows)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    os.makedirs(SAMPLES_DIR, exist_ok=True)

    files = {
        "demo_iracing.csv": gen_iracing_no_gps(),
        "demo_assetto_corsa.csv": gen_ac_no_gps(),
        "demo_iracing_gps.csv": gen_iracing_gps(),
        "demo_assetto_corsa_pos.csv": gen_ac_gps(),
    }

    for name, content in files.items():
        path = os.path.join(SAMPLES_DIR, name)
        with open(path, "w", newline="\n") as f:
            f.write(content)
        lines = content.count("\n")
        print(f"  {name}: {lines} filas (+ cabecera)")

    print("Listo. Archivos en docs/samples/")
