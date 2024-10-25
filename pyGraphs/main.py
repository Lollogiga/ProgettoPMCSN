import os

import carParkRicMu
import distribution
from graphicDraw import plot_finite_graph, plot_infinite_graph, plot_histogram

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


def findDistribution(verbose=True):
    file_csv = resultsPath + "finiteStradaLambda.csv"
    tasso_medio = distribution.calcola_tasso_arrivo_medio(file_csv)

    print(f"Tasso di arrivo medio: {tasso_medio * 60 * 60}")

    # Corregge l'input
    # graphicDraw.plot_distributions(file_csv, resultsPath + finiteSimFolder + distrAnalyses, "distributions.png")

    # Lista completa delle distribuzioni controllabili in fitter
    # [
    #     "alpha", "anglit", "arcsine", "argus", "beta", "betaprime", "bradford",
    #     "burr", "burr12", "cauchy", "chi", "chi2", "cosine", "crystalball",
    #     "dgamma", "dweibull", "erlang", "expon", "exponnorm", "exponpow",
    #     "exponweib", "f", "fatiguelife", "fisk", "foldcauchy", "foldnorm",
    #     "gamma", "gausshyper", "genexpon", "genextreme", "gengamma",
    #     "genhalflogistic", "geninvgauss", "genlogistic", "gennorm", "genpareto",
    #     "gompertz", "gumbel_r", "gumbel_l", "halfcauchy",
    #     "halflogistic", "halfnorm", "halfgennorm",
    #     "invgamma", "invgauss", "invweibull", "johnsonsb", "johnsonsu", "kappa3",
    #     "kappa4", "ksone"
    # ]

    fitterDistributions = [
        "beta", "betaprime", "burr12",
        "erlang", "expon", "exponnorm",
        "exponweib", "f", "gamma",
        "genexpon", "gengamma", "geninvgauss",
        "genpareto", "gompertz", "halfgennorm"
    ]

    distribution.fitterAnalyses(file_csv, fitterDistributions, verbose)

    beta = (0.9458757434103815, 17.385590063752446, 0.0004177570990577805, 92.6218429671368)
    betaprime = (0.993708762997116, 71.35971931345247, 0.0002580530235058, 339.02402458903805)
    burr12 = (1.0154021616910573, 17495468.144193314, 0.000409080659680104, 65170303.902252376)
    erlang = (1, 0.00041753112791715556, 4.736085961297903)
    expon = (0.0004177570990577806, 4.77909337986872)
    exponnorm = (806.3855077418712, 0.0017041692385299768, 0.005899873209683335)
    exponweib = (0.835221587271488, 1.1092978341310407, 0.0004177570990577805, 5.544836858613261)
    f = (1.9810234910086824, 2577.4925785715523, 0.00041772443600882785, 4.775768564018195)
    gamma = (0.9865801228655219, 0.0004177570962464504, 4.835085999939048)
    genexpon = (2.579644800617486, 0.7852315803966134, 4.364749543525269e-06, 0.00041775256012777757, 12.33057474665453)
    gengamma = (0.9724038186072996, 1.0119539394104955, 0.0004177570990577805, 4.936645210863208)
    geninvgauss = (0.9934840822469093, 2.594207449793989e-09, 0.00041775709905645774, 6.245004397344531e-09)
    genpareto = (0.003915715571291904, 0.00041775641115705787, 4.760386893006041)
    gompertz = (255303544099.21167, 0.0004177570990400645, 1242168711737.2544)
    halfgennorm = (0.9919054410919741, 0.00041775708964725917, 4.72693780899268)

    distrArgs = [
        beta, betaprime, burr12,
        erlang, expon, exponnorm,
        exponweib, f, gamma,
        genexpon, gengamma, geninvgauss,
        genpareto, gompertz, halfgennorm
    ]

    distribution.test_kolmogorov_smirnov(file_csv, fitterDistributions, distrArgs, verbose)


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

    if os.path.exists(resultsPath + "finiteStradaLambda.csv"):
        plot_histogram(resultsPath + "finiteStradaLambda.csv", resultsPath + finiteSimFolder + distrAnalyses, "histogram.png")
        findDistribution(verbose=True)

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
