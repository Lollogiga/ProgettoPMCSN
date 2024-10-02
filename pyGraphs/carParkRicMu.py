import pandas as pd

def calcola_mu_fin(file_path):
    # Leggi il file CSV
    df = pd.read_csv(file_path)

    # Inizializza variabili per accumulare le medie
    mu_parcheggio_totale = 0
    mu_ricarica_totale = 0
    seed_count = 0

    # Itera sui gruppi per ogni Seed
    for seed, group in df.groupby('Seed'):
        # Calcola la differenza tra 'Taken Time' e 'Completion Time' per ogni riga
        group['Service Time'] = group['Taken Time'] - group['Completion Time']

        # Filtra i dati per il centro 'Parcheggio' e 'Ricarica'
        df_parcheggio = group[group['Center'] == 'Parcheggio']
        df_ricarica = group[group['Center'] == 'Ricarica']

        # Calcola il tempo medio di servizio per ogni centro
        average_service_time_parcheggio = df_parcheggio['Service Time'].mean()
        average_service_time_ricarica = df_ricarica['Service Time'].mean()

        # Calcola il tasso mu come l'inverso del tempo medio di servizio
        mu_parcheggio = 1 / average_service_time_parcheggio if average_service_time_parcheggio > 0 else 0
        mu_ricarica = 1 / average_service_time_ricarica if average_service_time_ricarica > 0 else 0

        # Accumula i risultati
        mu_parcheggio_totale += mu_parcheggio
        mu_ricarica_totale += mu_ricarica
        seed_count += 1

    # Calcola la media tra le medie ottenute per ciascun seed
    mu_parcheggio_media = mu_parcheggio_totale / seed_count if seed_count > 0 else 0
    mu_ricarica_media = mu_ricarica_totale / seed_count if seed_count > 0 else 0

    return mu_parcheggio_media, mu_ricarica_media

def calcola_mu_inf(file_path):
    # Leggi il file CSV
    df = pd.read_csv(file_path)

    # Calcola la differenza tra 'Taken Time' e 'Completion Time' per ogni riga
    df['Service Time'] = df['Taken Time'] - df['Completion Time']

    # Filtra i dati per il centro 'Parcheggio' e 'Ricarica'
    df_parcheggio = df[df['Center'] == 'Parcheggio']
    df_ricarica = df[df['Center'] == 'Ricarica']

    # Calcola il tempo medio di servizio per ogni centro
    average_service_time_parcheggio = df_parcheggio['Service Time'].mean()
    average_service_time_ricarica = df_ricarica['Service Time'].mean()

    # Calcola il tasso mu come l'inverso del tempo medio di servizio
    mu_parcheggio = 1 / average_service_time_parcheggio if average_service_time_parcheggio > 0 else 0
    mu_ricarica = 1 / average_service_time_ricarica if average_service_time_ricarica > 0 else 0

    return mu_parcheggio, mu_ricarica
