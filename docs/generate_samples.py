"""
Genera CSVs de telemetría sintéticos pero realistas para ApexMetrics.
Uso: python3 docs/generate_samples.py
Salida: docs/samples/*.csv

Circuitos GPS:
  - demo_iracing_spa.csv      → Circuit de Spa-Francorchamps (Bélgica)
  - demo_iracing_monza.csv    → Autodromo Nazionale di Monza (Italia)
Circuitos genéricos (sin GPS):
  - demo_iracing.csv          → iRacing sin GPS, 2 vueltas
  - demo_assetto_corsa.csv    → Assetto Corsa sin GPS, 2 vueltas
  - demo_assetto_corsa_pos.csv → Assetto Corsa con posición local, 2 vueltas
"""
import math
import os

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
# 3. demo_iracing_spa.csv  —  iRacing GPS, Circuit de Spa-Francorchamps
#    ~400 puntos, 2 vueltas. Waypoints trazados con ~43 puntos de control
#    cubriendo todos los sectores del circuito belga.
# ---------------------------------------------------------------------------

SPA_WAYPOINTS = [
    # (lat, lon, speed_km_h) — sentido horario
    # ── Recta principal / Start-Finish ────────────────────
    (50.4372, 5.9714, 220),   # línea de meta
    (50.4375, 5.9709, 160),   # frenada La Source
    (50.4376, 5.9703, 80),    # La Source entrada
    (50.4378, 5.9695, 70),    # La Source ápex (horquilla derecha)
    (50.4374, 5.9687, 80),    # La Source salida
    # ── Descenso a Eau Rouge ──────────────────────────────
    (50.4365, 5.9677, 100),   # descenso
    (50.4355, 5.9670, 115),   # curva izquierda antes de Eau Rouge
    (50.4348, 5.9668, 105),   # Eau Rouge entrada
    (50.4343, 5.9663, 95),    # Eau Rouge ápex (izquierda)
    # ── Raidillon (subida) ────────────────────────────────
    (50.4346, 5.9651, 130),   # giro derecha al inicio subida
    (50.4356, 5.9633, 170),   # mitad de la subida
    (50.4366, 5.9616, 200),   # subida final
    (50.4378, 5.9601, 230),   # Raidillon cima / exit
    # ── Recta de Kemmel ──────────────────────────────────
    (50.4390, 5.9581, 270),
    (50.4408, 5.9560, 300),
    (50.4425, 5.9540, 315),
    (50.4441, 5.9527, 310),   # fin Kemmel / frenada Les Combes
    # ── Les Combes ────���───────────────────────────────────
    (50.4451, 5.9524, 100),   # Les Combes giro 1 (derecha)
    (50.4455, 5.9515, 85),    # Les Combes giro 2 (derecha)
    (50.4452, 5.9503, 95),    # Les Combes giro 3 (izquierda)
    # ── Malmedy / descenso ───────────────────────────────
    (50.4444, 5.9480, 165),
    (50.4438, 5.9455, 200),
    (50.4431, 5.9432, 215),
    # ── Rivage ────────────────────────────────────────────
    (50.4413, 5.9405, 140),   # frenada Rivage
    (50.4393, 5.9379, 80),    # Rivage entrada
    (50.4379, 5.9372, 75),    # Rivage ápex (horquilla izquierda)
    (50.4365, 5.9385, 100),   # Rivage salida
    (50.4347, 5.9410, 140),   # descenso post-Rivage
    # ── Pouhon ───────────────────────────────────────────
    (50.4330, 5.9432, 170),
    (50.4317, 5.9450, 200),   # Pouhon entrada (izquierda rápida)
    (50.4308, 5.9457, 215),   # Pouhon ápex
    (50.4300, 5.9466, 210),   # Pouhon salida
    # ── Fagnes / Stavelot ────────────────────────────────
    (50.4286, 5.9503, 200),   # Fagnes
    (50.4277, 5.9535, 130),   # frenada Stavelot
    (50.4272, 5.9558, 90),    # Stavelot entrada
    (50.4271, 5.9568, 85),    # Stavelot ápex (izquierda)
    (50.4274, 5.9580, 110),   # Stavelot salida
    # ── Paul Frere / recta ───────────────────────────────
    (50.4278, 5.9613, 220),
    (50.4282, 5.9647, 270),
    # ── Blanchimont ───────────────────────────────────────
    (50.4284, 5.9679, 285),   # Blanchimont inicio (izquierda rápida)
    (50.4286, 5.9700, 275),
    # ── Bus Stop chicane ─────────────────────────────────
    (50.4295, 5.9712, 150),   # frenada Bus Stop
    (50.4307, 5.9720, 90),    # Bus Stop izquierda
    (50.4313, 5.9717, 85),    # Bus Stop derecha
    (50.4323, 5.9715, 110),   # Bus Stop salida
    # ── Regreso a meta ───────────────────────────────────
    (50.4345, 5.9714, 160),
    (50.4372, 5.9714, 220),   # cierra circuito
]


def interpolate_gps_track(waypoints, n_points_total):
    n_wps = len(waypoints)
    seg_lengths = []
    for i in range(n_wps - 1):
        dlat = waypoints[i+1][0] - waypoints[i][0]
        dlon = waypoints[i+1][1] - waypoints[i][1]
        seg_lengths.append(math.sqrt(dlat**2 + dlon**2))
    total_len = sum(seg_lengths)

    result = []
    for seg_i in range(n_wps - 1):
        seg_pts = max(2, round(n_points_total * seg_lengths[seg_i] / total_len))
        lat0, lon0, v0 = waypoints[seg_i]
        lat1, lon1, v1 = waypoints[seg_i + 1]
        for k in range(seg_pts):
            t = k / seg_pts
            e = t * t * (3 - 2 * t)
            result.append((lat0 + (lat1 - lat0) * e, lon0 + (lon1 - lon0) * e, v0 + (v1 - v0) * e))

    while len(result) < n_points_total:
        result.append(result[-1])
    return result[:n_points_total]


def gen_iracing_gps_circuit(waypoints, n_per_lap, n_laps, filename_hint=""):
    track = interpolate_gps_track(waypoints, n_per_lap)
    speeds = [p[2] for p in track]

    window = 4
    smoothed = []
    for i in range(n_per_lap):
        s = sum(speeds[(i + k) % n_per_lap] for k in range(-window, window + 1))
        smoothed.append(s / (2 * window + 1))

    brakes, throttles = derive_brake_throttle(smoothed)

    R = 6371000
    rows = ["Distance,Speed,Brake,Throttle,Lat,Lon"]
    for _lap in range(n_laps):
        prev_lat, prev_lon = track[0][0], track[0][1]
        dist_lap = 0.0
        for i in range(n_per_lap):
            lat, lon, _ = track[i]
            dlat = math.radians(lat - prev_lat)
            dlon = math.radians(lon - prev_lon)
            a = (math.sin(dlat/2)**2
                 + math.cos(math.radians(prev_lat))
                 * math.cos(math.radians(lat))
                 * math.sin(dlon/2)**2)
            dist_lap += R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
            prev_lat, prev_lon = lat, lon
            rows.append(
                f"{dist_lap:.1f},{smoothed[i]:.1f},{brakes[i]:.3f},{throttles[i]:.3f},"
                f"{lat:.6f},{lon:.6f}"
            )
        dist_lap = 0.0  # iRacing resetea distancia en cada vuelta

    return "\n".join(rows)


# ---------------------------------------------------------------------------
# 4. demo_iracing_monza.csv  —  iRacing GPS, Autodromo Nazionale di Monza
#    Circuito clásico de ~5.793 km, sentido antihorario.
# ---------------------------------------------------------------------------

MONZA_WAYPOINTS = [
    # (lat, lon, speed_km_h)
    # ── Recta principal ───────────────────────────────────
    (45.6156, 9.2811, 280),   # Start/Finish
    (45.6186, 9.2815, 315),
    (45.6218, 9.2818, 340),   # final recta
    # ── Prima Variante (chicane) ──────────────────────────
    (45.6238, 9.2820, 90),    # frenada Prima Variante
    (45.6243, 9.2826, 80),    # Prima Variante izquierda
    (45.6240, 9.2831, 75),    # Prima Variante derecha
    (45.6234, 9.2823, 100),   # salida Prima Variante
    # ── Curva Grande ─────────────────────────────────────
    (45.6220, 9.2803, 200),
    (45.6207, 9.2780, 230),   # Curva Grande (derecha rápida)
    (45.6197, 9.2763, 245),
    # ── Roggia (Seconda Variante) ─────────────────────────
    (45.6191, 9.2768, 100),   # frenada Roggia
    (45.6187, 9.2776, 90),    # Roggia izquierda
    (45.6190, 9.2786, 100),   # Roggia derecha
    # ── Lesmo 1 y 2 ──────────────────────────────────────
    (45.6181, 9.2820, 200),   # aproximación Lesmos
    (45.6172, 9.2838, 110),   # frenada Lesmo 1
    (45.6165, 9.2846, 105),   # Lesmo 1 ápex (derecha)
    (45.6162, 9.2857, 108),   # Lesmo 2 entrada
    (45.6163, 9.2866, 120),   # Lesmo 2 ápex (derecha)
    # ── Recta del Serraglio ───────────────────────────────
    (45.6173, 9.2877, 230),
    (45.6185, 9.2889, 265),
    (45.6197, 9.2900, 285),   # final Serraglio
    # ── Variante Ascari (chicane) ─────────────────────────
    (45.6203, 9.2905, 85),    # frenada Ascari
    (45.6207, 9.2899, 80),    # Ascari izquierda
    (45.6202, 9.2891, 90),    # Ascari derecha
    (45.6196, 9.2883, 110),   # Ascari salida
    # ── Recta antes de Parabolica ─────────────────────────
    (45.6184, 9.2866, 270),
    (45.6172, 9.2849, 290),
    # ── Parabolica (curva derecha larga) ─────────────────
    (45.6161, 9.2841, 120),   # frenada Parabolica
    (45.6152, 9.2833, 115),   # Parabolica ápex
    (45.6146, 9.2820, 150),   # Parabolica salida
    # ── Regreso a meta ────────────────────────────────────
    (45.6148, 9.2812, 210),
    (45.6156, 9.2811, 280),   # cierra circuito
]


# ---------------------------------------------------------------------------
# 5. demo_assetto_corsa_pos.csv  —  AC con posición local, circuito de kart
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
        "demo_iracing.csv":           gen_iracing_no_gps(),
        "demo_assetto_corsa.csv":     gen_ac_no_gps(),
        "demo_iracing_spa.csv":       gen_iracing_gps_circuit(SPA_WAYPOINTS, 200, 2),
        "demo_iracing_monza.csv":     gen_iracing_gps_circuit(MONZA_WAYPOINTS, 200, 2),
        "demo_assetto_corsa_pos.csv": gen_ac_gps(),
    }

    for name, content in files.items():
        path = os.path.join(SAMPLES_DIR, name)
        with open(path, "w", newline="\n") as f:
            f.write(content)
        lines = content.count("\n")
        print(f"  {name}: {lines} filas (+ cabecera)")

    print("Listo. Archivos en docs/samples/")
