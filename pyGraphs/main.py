import os

import distribution
from graphicDraw import plot_finite_graph, plot_infinite_graph, plot_histogram, plot_distributions

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

    # Lista completa delle distribuzioni controllabili in fitter
    # fitterDistributions = [
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

    # Data for simulation with 0 cars and STOP_FIN = 86400
    # fitterDistributions = [
    #     "beta", "betaprime", "burr", "burr12",
    #     "erlang", "expon", "exponnorm",
    #     "exponweib", "f", "gamma",
    #     "genexpon", "gengamma", "geninvgauss",
    #     "genpareto", "gompertz", "halfgennorm",
    #     "weibull_min"
    # ]

    # Data for simulation with 0 cars and STOP_FIN = 21600
    fitterDistributions = [
        "beta", "betaprime", "burr", "burr12", "chi2",
        "erlang", "expon", "exponnorm", "exponweib", "f",
        "gengamma", "genexpon", "geninvgauss", "genpareto",
        "gompertz", "halfgennorm"
    ]

    distribution.fitterAnalyses(file_csv, fitterDistributions, verbose)

    # Data for simulation with 0 cars and STOP_FIN = 86400
    # beta        = (0.9433094421718106, 171.47345236731925, 0.00012155074728070757, 1003.5322001716285)
    # betaprime   = (0.9970983784550285, 15.573293366130493, 0.00012154960770958053, 80.1144261273984)
    # burr        = (2.6112207822400673, 0.31616778551970814, 0.0001215507471925167, 8.399895334549445)
    # burr12      = (1.006061877865103, 14.108538806668816, 0.0001193790497589579, 70.94778969692513)
    # erlang      = (1, 0.00012155074728070753, 5.811175392553608)
    # expon       = (0.00012155074728070758, 5.484068831251863)
    # exponnorm   = (812.244836235591, 0.0016437713346312388, 0.006746955093765439)
    # exponweib   = (1.1337494314505643, 0.8969659112867497, 0.00011478665104952334, 4.78314186887436)
    # f           = (1.8990745898866834, 99.28817354972702, 0.00012155074728070757, 5.3407450152111515)
    # gamma       = (0.33207872728082855, 0.00012155074728070757, 2.6881476718517785)
    # genexpon    = (2.2747832414617233, 1.4867882714207101, 4.1000270679237557e-08, 0.00012154213013190945, 12.474245016130467)
    # gengamma    = (1.1506571654827493, 0.8844230805820488, 0.00011447316720972476, 4.433116469260721)
    # geninvgauss = (0.9617828899249844, 8.720979401373946e-09, 0.00012155074727929129, 2.4849380638122966e-08)
    # genpareto   = (0.06535235797954206, 0.00012154856323668507, 5.1232845409146055)
    # gompertz    = (303231868731.4423, 0.00012155074720403157, 1536738887354.7852)
    # halfgennorm = (0.902434246373263, 0.00012155063883214525, 4.676852542574426)
    # weibull_min = (0.9592286017868228, 0.00012155074728070756, 5.406412590083644)

    # Data for simulation with 0 cars and STOP_FIN = 86400
    # distrArgs = [
    #     beta, betaprime, burr, burr12,
    #     erlang, expon, exponnorm,
    #     exponweib, f, gamma,
    #     genexpon, gengamma, geninvgauss,
    #     genpareto, gompertz, halfgennorm,
    #     weibull_min
    # ]

    # Data for simulation with 0 cars and STOP_FIN = 21600
    beta            = (0.9065145610724696, 106.70023030106152, 0.0006240784514375263, 901.1116638491368)
    betaprime       = (0.9953192767312321, 8.940034928431354, 0.0006240784514371983, 60.503446917064274)
    burr            = (2.5385859540513316, 0.33210114246789046, 0.0006240784514375263, 11.26666042702382)
    burr12          = (1.0091995857382052, 8.025305788452137, 0.0005996463790749959, 52.525106363026424)
    chi2            = (1.763128606088761, 0.0006240784514375263, 4.278212155971736)
    erlang          = (1, 0.0006240784514375263, 8.178569034601768)
    expon           = (0.0006240784514375264, 7.580846148699907)
    exponnorm       = (1003.9452640621839, -0.0021935881649236427, 0.007579492018979624)
    exponweib       = (1.2585538466059571, 0.8237221929141614, 0.0005049539823119079, 5.825907495819535)
    f               = (1.97753681421089, 16.206032568805213, 0.0006240784514375263, 6.671722087642494)
    gengamma        = (1.1227287779568138, 0.8697691506328915, 0.0006240784514375263, 6.249940066444157)
    genexpon        = (2.157695810735323, 1.761921498073571, 5.115092502970153e-10, 0.0006240783620939909, 16.357199546672025)
    geninvgauss     = (0.9170917993211929, 1.219650267018608e-08, 0.0006240784514075729, 5.050515837228168e-08)
    genpareto       = (0.11484414547813493, 0.0006240783791455567, 6.706135926087843)
    gompertz        = (387044629287.61316, 0.0006240784498636317, 2645852550589.635)
    halfgennorm     = (0.8358184504913837, 0.0006240784301526734, 5.631155077562735)

    # Data for simulation with 0 cars and STOP_FIN = 21600
    distrArgs = [
        beta, betaprime, burr, burr12, chi2,
        erlang, expon, exponnorm, exponweib, f,
        gengamma, genexpon, geninvgauss, genpareto,
        gompertz, halfgennorm
    ]

    distribution.test_kolmogorov_smirnov(file_csv, fitterDistributions, distrArgs, verbose)

    img_folder = resultsPath + finiteSimFolder + distrAnalyses

    onlyBestNames = [
        "gengamma", "burr12", "betaprime", "genpareto", "exponweib"
    ]

    onlyBest = [
        gengamma, burr12, betaprime, genpareto, exponweib
    ]

    plot_distributions(file_csv, onlyBestNames, onlyBest, img_folder, "istogram.png")


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

    # fileCarMu_csv = resultsPath + "infiniteCarMu.csv"
    # if os.path.exists(fileCarMu_csv):
    #     mu_parcheggio, mu_ricarica = carParkRicMu.calcola_mu_inf(fileCarMu_csv)
    #     print("\nCaso di studio INFINITO")
    #     print(f"Tasso mu per la stazione di parcheggio: {mu_parcheggio} job per unità di tempo")
    #     print(f"Tasso mu per la stazione di ricarica: {mu_ricarica} job per unità di tempo")
    #
    # fileCarMu_csv = resultsPath + "finiteCarMu.csv"
    # if os.path.exists(fileCarMu_csv):
    #     mu_parcheggio, mu_ricarica = carParkRicMu.calcola_mu_fin(fileCarMu_csv)
    #     print("\nCaso di studio FINITO")
    #     print(f"Tasso mu per la stazione di parcheggio: {mu_parcheggio} job per unità di tempo")
    #     print(f"Tasso mu per la stazione di ricarica: {mu_ricarica} job per unità di tempo")


if __name__ == "__main__":
    main()
