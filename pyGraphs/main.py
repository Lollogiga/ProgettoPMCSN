import os
from graphicDraw import plot_combined_graph
from distribution import calcola_tasso_arrivo_medio

baseFolder = os.path.abspath(os.path.join(os.getcwd(), os.pardir))

resultsPath = baseFolder + "/" + "resources/results/"
finiteSimFolder = "finiteSimImg/"

def finiteSimGraphs(selected_seeds):
    finiteNoleggio = resultsPath + "finiteNoleggio.csv"
    finiteStrada = resultsPath + "finiteStrada.csv"
    finiteParcheggio = resultsPath + "finiteParcheggio.csv"
    finiteRicarica = resultsPath + "finiteRicarica.csv"

    plot_combined_graph(finiteNoleggio, selected_seeds, resultsPath + finiteSimFolder, "noleggio.png", "Noleggio")
    plot_combined_graph(finiteStrada, selected_seeds, resultsPath + finiteSimFolder, "strada.png", "Strada")
    plot_combined_graph(finiteParcheggio, selected_seeds, resultsPath + finiteSimFolder, "parcheggio.png", "Parcheggio")
    plot_combined_graph(finiteRicarica, selected_seeds, resultsPath + finiteSimFolder, "ricarica.png", "Ricarica")

def main():
    selected_seeds = [123456789, 382880042, 484764695, 624212696, 719463368, 928379944]  # 6 seeds
    finiteSimGraphs(selected_seeds)

    file_csv = resultsPath + "finiteStradaLambda.csv"
    tasso_medio = calcola_tasso_arrivo_medio(file_csv)

    print(f"Tasso di arrivo medio: {tasso_medio}")

    # infiniteSimFolder = "D:\\Projects\\IdeaProjects\\ProgettoPMCSN\\resources\\results\\infinite_horizon"


if __name__ == "__main__":
    main()

