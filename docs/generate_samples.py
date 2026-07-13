"""
Genera CSVs de telemetría sintéticos pero realistas para ApexMetrics.
Uso: python3 docs/generate_samples.py
Salida: docs/samples/*.csv

Circuitos GPS (trazado REAL desde OpenStreetMap, ver docs/track_geometry.py):
  - demo_iracing_spa.csv        → Circuit de Spa-Francorchamps (Bélgica)
  - demo_iracing_monza.csv      → Autodromo Nazionale di Monza (Italia)
  - demo_iracing_monza_real.csv → export "real" de alta fidelidad (Monza)
Circuitos genéricos (sin GPS):
  - demo_iracing.csv           → iRacing sin GPS, 2 vueltas
  - demo_assetto_corsa.csv     → Assetto Corsa sin GPS, 2 vueltas
  - demo_assetto_corsa_pos.csv → Assetto Corsa con posición local, 2 vueltas

Nota sobre los CSV GPS: el TRAZADO (lat/lon) proviene de la geometría real de
OpenStreetMap, por lo que calza exacto sobre los tiles del mapa. La VELOCIDAD y
los canales de freno/acelerador son un perfil sintético derivado por curvatura
(no telemetría real del simulador). Ver docs/BUG_CSV_TRAZADOS.md, Causa C.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from track_geometry import MONZA_CENTERLINE, SPA_CENTERLINE  # noqa: E402

SAMPLES_DIR = os.path.join(os.path.dirname(__file__), "samples")


def lerp(a, b, t):
    return a + (b - a) * t


def smooth_clamp(v, lo, hi):
    return max(lo, min(hi, v))


def build_speed_profile(waypoints, total_dist, n_points):
    speeds = []
    for i in range(n_points):
        d = (i / n_points) * total_dist
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
                span = total_dist
                frac = ((d - prev[0]) % total_dist) / span
            else:
                frac = (d - prev[0]) / span
            t = frac * frac * (3 - 2 * frac)
            speeds.append(lerp(prev[1], nxt[1], t))

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
    n = len(speeds)
    brakes = []
    throttles = []
    for i in range(n):
        prev_v = speeds[(i - 1) % n]
        curr_v = speeds[i]
        delta = curr_v - prev_v
        max_delta = 15.0

        if delta < -2.0:
            b = smooth_clamp(-delta / max_delta * 1.2, 0.0, 1.0)
            t = 0.0
        elif delta > 1.0:
            b = 0.0
            t = smooth_clamp(delta / max_delta, 0.1, 1.0)
        else:
            b = 0.0
            t = smooth_clamp(curr_v / 280.0, 0.3, 0.8)

        brakes.append(round(b, 3))
        throttles.append(round(t, 3))

    return brakes, throttles


# ---------------------------------------------------------------------------
# 1. demo_iracing.csv  —  iRacing sin GPS, ~300 puntos, 2 vueltas
# ---------------------------------------------------------------------------

def gen_iracing_no_gps():
    total_dist_per_lap = 5800
    n_per_lap = 150
    n_laps = 2

    waypoints = [
        (0, 120), (100, 250), (500, 280), (900, 310),
        (1000, 80), (1200, 180), (1500, 220), (1800, 260),
        (1900, 60), (2100, 160), (2400, 210), (2700, 240),
        (2800, 90), (3000, 200), (3300, 260), (3600, 290),
        (3700, 70), (3900, 180), (4200, 230), (4300, 110),
        (4500, 200), (4800, 270), (5100, 300), (5200, 85),
        (5500, 200), (5800, 120),
    ]
    waypoints = [(d * total_dist_per_lap / 5800, v) for d, v in waypoints]

    speeds_lap = build_speed_profile(waypoints, total_dist_per_lap, n_per_lap)
    brakes_lap, throttles_lap = derive_brake_throttle(speeds_lap)

    rows = ["Distance,Speed,Brake,Throttle"]
    for _lap in range(n_laps):
        for i in range(n_per_lap):
            dist = i * (total_dist_per_lap / n_per_lap)
            rows.append(f"{dist:.1f},{speeds_lap[i]:.1f},{brakes_lap[i]:.3f},{throttles_lap[i]:.3f}")

    return "\n".join(rows)


# ---------------------------------------------------------------------------
# 2. demo_assetto_corsa.csv  —  AC sin GPS
# ---------------------------------------------------------------------------

def gen_ac_no_gps():
    n_per_lap = 150
    n_laps = 2

    waypoints = [
        (0, 100), (10, 220), (30, 260), (40, 55), (55, 140),
        (70, 200), (90, 240), (100, 40), (115, 150), (130, 210),
        (140, 70), (150, 120), (n_per_lap, 100),
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
# 3-5. CSV GPS con trazado REAL (Monza, Spa) desde OpenStreetMap
#      El centerline (lat/lon) es geometría real; la velocidad es sintética
#      derivada por curvatura. Ver docs/track_geometry.py y BUG_CSV_TRAZADOS.md.
# ---------------------------------------------------------------------------

_EARTH_R = 6371000.0


def haversine(a, b):
    """Distancia en metros entre dos (lat, lon)."""
    p1, p2 = math.radians(a[0]), math.radians(b[0])
    dphi = math.radians(b[0] - a[0])
    dl = math.radians(b[1] - a[1])
    h = math.sin(dphi / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return _EARTH_R * 2 * math.atan2(math.sqrt(h), math.sqrt(1 - h))


def bearing_rad(a, b):
    """Rumbo (radianes) del segmento a→b."""
    lat1, lat2 = math.radians(a[0]), math.radians(b[0])
    dl = math.radians(b[1] - a[1])
    x = math.sin(dl) * math.cos(lat2)
    y = math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(dl)
    return math.atan2(x, y)


def resample_closed(centerline, n):
    """Re-muestrea un lazo cerrado (lista de (lat, lon)) a n puntos equiespaciados
    por longitud de arco. Devuelve (puntos, longitud_total_m)."""
    pts = list(centerline)
    if haversine(pts[0], pts[-1]) > 1.0:
        pts = pts + [pts[0]]  # cierra el lazo
    cum = [0.0]
    for i in range(1, len(pts)):
        cum.append(cum[-1] + haversine(pts[i - 1], pts[i]))
    total = cum[-1]
    out = []
    for k in range(n):
        d = total * k / n
        j = 0
        while j < len(cum) - 1 and cum[j + 1] < d:
            j += 1
        seg = (cum[j + 1] - cum[j]) or 1.0
        t = (d - cum[j]) / seg
        la = pts[j][0] + (pts[j + 1][0] - pts[j][0]) * t
        lo = pts[j][1] + (pts[j + 1][1] - pts[j][1]) * t
        out.append((la, lo))
    return out, total


def curvature_speed(track, vmin, vmax, win=2, smooth=3):
    """Perfil de velocidad por curvatura: acumula el cambio de rumbo en una
    ventana corta (una chicane corta debe registrar como muy lenta) y lo mapea
    a velocidad entre vmin y vmax, con suavizado para un perfil realista."""
    n = len(track)
    turn = [0.0] * n
    for i in range(n):
        acc = 0.0
        for k in range(-win, win):
            a = track[(i + k) % n]
            b = track[(i + k + 1) % n]
            c = track[(i + k + 2) % n]
            acc += abs((bearing_rad(b, c) - bearing_rad(a, b) + math.pi) % (2 * math.pi) - math.pi)
        turn[i] = acc
    cap = max(0.5, sorted(turn)[int(n * 0.90)])
    inten = [min(1.0, t / cap) for t in turn]
    sm = []
    for i in range(n):
        s = sum(inten[(i + k) % n] for k in range(-smooth, smooth + 1))
        sm.append(s / (2 * smooth + 1))
    speed = [vmax - (vmax - vmin) * (x ** 0.9) for x in sm]
    out = []
    for i in range(n):
        s = sum(speed[(i + k) % n] for k in range(-smooth, smooth + 1))
        out.append(s / (2 * smooth + 1))
    return out


def _derive_bt_gps(speeds, vmax=320.0):
    """Freno/acelerador realistas a partir del perfil de velocidad.

    El freno sigue la INTENSIDAD de desaceleración (pico en la frenada fuerte y
    liberación progresiva al bajar de marcha hacia el ápex — no una meseta al
    100%). El acelerador es alto en recta/aceleración y se levanta en la frenada.
    Ambos se escalan de forma robusta (percentil 92) y se suavizan para eliminar
    los escalones on/off que producía el umbral binario anterior."""
    n = len(speeds)
    accel = [speeds[i] - speeds[(i - 1) % n] for i in range(n)]  # dv por muestra
    dec = [max(0.0, -a) for a in accel]  # magnitud de frenada
    acc = [max(0.0, a) for a in accel]   # magnitud de aceleración

    positivos = sorted(d for d in dec if d > 0.0)
    cap = positivos[int(len(positivos) * 0.92)] if positivos else 1.0
    cap = max(cap, 1e-3)

    brakes = [min(1.0, d / cap) ** 0.9 for d in dec]
    throttles = []
    for i in range(n):
        if brakes[i] > 0.05:
            throttles.append(0.0)                                  # no coexisten
        elif acc[i] > 0.02:
            throttles.append(min(1.0, 0.45 + acc[i] / cap))        # acelerando
        else:
            throttles.append(min(1.0, max(0.5, speeds[i] / vmax)))  # crucero en recta

    def _smooth(arr, w=2):
        return [sum(arr[(i + k) % n] for k in range(-w, w + 1)) / (2 * w + 1)
                for i in range(n)]

    brakes = _smooth(brakes, 2)
    throttles = _smooth(throttles, 2)
    return [round(b, 3) for b in brakes], [round(t, 3) for t in throttles]


def _gear_for_speed(v):
    """Marcha aproximada en función de la velocidad (km/h), rango 1..7."""
    gear = 1
    for th in (80, 120, 160, 200, 240, 280):
        if v > th:
            gear += 1
    return gear


def _rpm_for_speed(v, gear):
    """RPM plausible: sube con la velocidad dentro de la marcha, con leve variación."""
    base = 4000 + (v % 60) / 60.0 * 4500
    return int(smooth_clamp(base + (7 - gear) * 250, 3200, 8600))


def gen_iracing_gps_real(centerline, n_per_lap, n_laps, vmin, vmax, real_format=False):
    """Genera un CSV GPS de iRacing sobre un centerline REAL (lat/lon de OSM).

    real_format=False → columnas demo: Distance,Speed,Brake,Throttle,Lat,Lon
    real_format=True  → export "real": Lat,Lon,Speed,Throttle,Brake,Gear,RPM,Distance
                        (orden no canónico + columnas extra Gear/RPM que el parser
                        debe ignorar; ~360 pts/vuelta). Speed en km/h (contrato app).
    """
    track, _ = resample_closed(centerline, n_per_lap)
    speeds = curvature_speed(track, vmin, vmax)
    brakes, throttles = _derive_bt_gps(speeds, vmax)

    if real_format:
        rows = ["Lat,Lon,Speed,Throttle,Brake,Gear,RPM,Distance"]
    else:
        rows = ["Distance,Speed,Brake,Throttle,Lat,Lon"]

    for _lap in range(n_laps):
        prev = track[0]
        dist_lap = 0.0
        for i in range(n_per_lap):
            lat, lon = track[i]
            dist_lap += haversine(prev, track[i])
            prev = track[i]
            v = speeds[i]
            if real_format:
                gear = _gear_for_speed(v)
                rpm = _rpm_for_speed(v, gear)
                rows.append(
                    f"{lat:.6f},{lon:.6f},{v:.1f},{throttles[i]:.3f},{brakes[i]:.3f},"
                    f"{gear},{rpm},{dist_lap:.1f}"
                )
            else:
                rows.append(
                    f"{dist_lap:.1f},{v:.1f},{brakes[i]:.3f},{throttles[i]:.3f},"
                    f"{lat:.6f},{lon:.6f}"
                )
        # iRacing resetea la distancia en cada vuelta → dispara la detección de vuelta

    return "\n".join(rows)


# ---------------------------------------------------------------------------
# 6. demo_assetto_corsa_pos.csv  —  AC con posición local, circuito de kart
#    Formato: pos,speedKmh,brake,gas,posX,posZ — plano CRS.Simple
# ---------------------------------------------------------------------------

AC_TRACK_WAYPOINTS = [
    # (posX, posZ, speed) — circuito cerrado con 4 sectores
    (400, 50,  120),   # Start/Finish
    (600, 50,  280),   # recta principal
    (750, 80,  310),
    (780, 150,  90),   # curva 1 entrada
    (780, 250,  70),   # curva 1 ápex
    (760, 320, 180),   # curva 1 salida
    (700, 400, 240),   # sector técnico
    (640, 430, 180),
    (580, 450,  80),   # curva 2 (horquilla)
    (520, 440, 160),
    (450, 420, 200),   # recta posterior
    (350, 430, 220),
    (280, 410, 230),
    (220, 380, 100),   # curva 3 (chicane)
    (190, 350,  80),
    (200, 300, 160),
    (230, 260, 200),
    (200, 200, 240),
    (180, 150,  90),   # curva 4 (última antes de recta)
    (200, 100,  80),
    (250,  70, 160),
    (320,  52, 220),
    (400,  50, 120),   # cierra circuito
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
        "demo_iracing.csv":            gen_iracing_no_gps(),
        "demo_assetto_corsa.csv":      gen_ac_no_gps(),
        "demo_iracing_spa.csv":        gen_iracing_gps_real(SPA_CENTERLINE, 200, 2, 80, 315),
        "demo_iracing_monza.csv":      gen_iracing_gps_real(MONZA_CENTERLINE, 200, 2, 95, 338),
        "demo_iracing_monza_real.csv": gen_iracing_gps_real(MONZA_CENTERLINE, 360, 2, 95, 338, real_format=True),
        "demo_assetto_corsa_pos.csv":  gen_ac_gps(),
    }

    for name, content in files.items():
        path = os.path.join(SAMPLES_DIR, name)
        with open(path, "w", newline="\n") as f:
            f.write(content)
        lines = content.count("\n")
        print(f"  {name}: {lines} filas (+ cabecera)")

    print("Listo. Archivos en docs/samples/")
