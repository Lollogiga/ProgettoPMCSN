import os
from graphicDraw import plot_finite_graph, plot_infinite_graph
import carParkRicMu

baseFolder = os.path.abspath(os.path.join(os.getcwd(), os.pardir))

resultsPath = baseFolder + "/" + "resources/results/"
finiteSimFolder = "finiteSimImg/"
infiniteSimFolder = "infiniteSimImg/"
distrAnalyses = "distrAnalyses/"

def finiteSimGraphs(selected_seeds):
    finiteNoleggio = resultsPath + "finiteNoleggio.csv"
    finiteStrada = resultsPath + "finiteStrada.csv"
    finiteParcheggio = resultsPath + "finiteParcheggio.csv"
    finiteRicarica = resultsPath + "finiteRicarica.csv"

    plot_finite_graph(finiteNoleggio, selected_seeds, resultsPath + finiteSimFolder, "noleggio.png", "Noleggio")
    plot_finite_graph(finiteStrada, selected_seeds, resultsPath + finiteSimFolder, "strada.png", "Strada")
    plot_finite_graph(finiteParcheggio, selected_seeds, resultsPath + finiteSimFolder, "parcheggio.png", "Parcheggio")
    plot_finite_graph(finiteRicarica, selected_seeds, resultsPath + finiteSimFolder, "ricarica.png", "Ricarica")

def infiniteSimGraphs():
    infiniteHorizonStatsNoleggio = resultsPath + "infiniteHorizonStatsNoleggio.csv"
    infiniteHorizonStatsStrada = resultsPath + "infiniteHorizonStatsStrada.csv"
    infiniteHorizonStatsParcheggio = resultsPath + "infiniteHorizonStatsParcheggio.csv"
    infiniteHorizonStatsRicarica = resultsPath + "infiniteHorizonStatsRicarica.csv"

    plot_infinite_graph(infiniteHorizonStatsNoleggio, resultsPath + infiniteSimFolder, "noleggio.png", "Noleggio")
    plot_infinite_graph(infiniteHorizonStatsStrada, resultsPath + infiniteSimFolder, "strada.png", "Strada")
    plot_infinite_graph(infiniteHorizonStatsParcheggio, resultsPath + infiniteSimFolder, "parcheggio.png", "Parcheggio")
    plot_infinite_graph(infiniteHorizonStatsRicarica, resultsPath + infiniteSimFolder, "ricarica.png", "Ricarica")


def main():
    selected_seeds = [123456789, 49341648, 624212696, 928379944, 382880042]  # 5 seeds
    if (os.path.exists(resultsPath + "finiteNoleggio.csv") and
            os.path.exists(resultsPath + "finiteStrada.csv") and
            os.path.exists(resultsPath + "finiteParcheggio.csv") and
            os.path.exists(resultsPath + "finiteRicarica.csv")):
        finiteSimGraphs(selected_seeds)
    elif (os.path.exists(resultsPath + "infiniteHorizonStatsNoleggio.csv") and
                os.path.exists(resultsPath + "infiniteHorizonStatsStrada.csv") and
                os.path.exists(resultsPath + "infiniteHorizonStatsParcheggio.csv") and
                os.path.exists(resultsPath + "infiniteHorizonStatsRicarica.csv")):
        infiniteSimGraphs()

    fileCarMu_csv = resultsPath + "infiniteCarMu.csv"
    if os.path.exists(fileCarMu_csv):
        mu_parcheggio, mu_ricarica = carParkRicMu.calcola_mu_inf(fileCarMu_csv)
        print("\nCaso di studio INFINITO")
        print(f"Tasso mu per la stazione di parcheggio: {mu_parcheggio} job per unità di tempo")
        print(f"Tasso mu per la stazione di ricarica: {mu_ricarica} job per unità di tempo")

    fileCarMu_csv = resultsPath + "finiteCarMu.csv"
    if os.path.exists(fileCarMu_csv):
        mu_parcheggio, mu_ricarica = carParkRicMu.calcola_mu_fin(fileCarMu_csv)
        print("\nCaso di studio FINITO")
        print(f"Tasso mu per la stazione di parcheggio: {mu_parcheggio} job per unità di tempo")
        print(f"Tasso mu per la stazione di ricarica: {mu_ricarica} job per unità di tempo")


if __name__ == "__main__":
    main()
