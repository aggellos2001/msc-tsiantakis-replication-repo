# The script assumes there is an excel file named Measurements.xlsx to read the measurements
# from. this is also provided in the github repository along with this code.
Measurements <- readxl::read_excel("../Measurements.xlsx")
# Also assumes you have a figures folder outside your folder where you current
# working directory is. (run getwd() to read your current
# working directory) - otherwise make sure to change the location to whatever
# folder you desire the figures to be saved on.
FIGURE_FOLDER <- "../figures"

save_plot <- function(filename) {
  ggplot2::ggsave(
    filename = file.path(FIGURE_FOLDER, filename),
    width = 12,
    height = 8,
    units = "cm",
    dpi = 300,
    compression = "lzw"
  )
}

format_p <- function(p) {
  ifelse(p < 0.001, "< 0,001", format(round(p, 3), decimal.mark = ","))
}

.plot_and_save <- function(.df, .x, .y, .filename, .title_column = NULL) {
 
  
  localdf <- .df |> dplyr::mutate(framework = factor(framework, levels = c("KMP", "Flutter", "Native")))
  
  filename <- NULL
  
  x_name <- rlang::as_label(rlang::enquo(.x))
  y_name <- rlang::as_label(rlang::enquo(.y))
  
  plot_title <- NULL
  if (!missing(.title_column)) {
    val <- localdf |> dplyr::pull({{ .title_column }}) |> unique()
    plot_title <- suffix_only <- sub("^[^:]+:", "", val)
    filename <- paste0(.filename, "_", plot_title |> unique(), "_", x_name , ".tiff")
  } else {
    filename <- paste0(.filename, "_", x_name, ".tiff")
  }
  
  get_label <- function(col_name) {
    switch(
      col_name,
      "app_displayed_ms" = "Time to Initial Display (ms)",
      "avg_benchmark_ms" = "Time to Complete (ms)",
      "peak_cpu_usage_perc" = "Peak CPU Usage (%)",
      "baselineRAM_mb" = "Baseline RAM (MB)",
      "benchmarkRAM_mb" = "Benchmark RAM (MB)",
      "framework" = "Framework",
      "benchmark" = "Benchmark Type",
      col_name
    )
  }
  
  .xlabel <- get_label(x_name)
  .ylabel <- get_label(y_name)
  
  ggplot2::ggplot(localdf, ggplot2::aes(x = {{.x}}, y = {{.y}})) +
    ggplot2::geom_boxplot(
      fill = "gray90",
      color = "black",
      outlier.shape = 1,
      outlier.size = 1,
      linewidth = 0.4
    ) +
    ggplot2::labs(x = .xlabel, y = .ylabel, title = plot_title) +
    ggplot2::theme_bw() +
    ggplot2::theme(
      plot.title = ggplot2::element_text(
        family = "serif",
        face = "bold",
        size = 12,
        hjust = 0.5,
        margin = ggplot2::margin(b = 10)
      ),
      text = ggplot2::element_text(family = "serif", size = 10),
      axis.title = ggplot2::element_text(face = "bold", size = 11),
      axis.text = ggplot2::element_text(color = "black"),
      panel.grid.major = ggplot2::element_blank(),
      panel.grid.minor = ggplot2::element_blank(),
      panel.border = ggplot2::element_rect(
        colour = "black",
        fill = NA,
        linewidth = 0.5
      ),
      axis.line = ggplot2::element_blank()
    )
  save_plot(filename)
  invisible(.df)
}



.descriptive_stats <- function(.df, .group_variable, .metric) {
  localdf <- .df |> dplyr::mutate(framework = factor(framework, levels = c("Native", "Flutter", "KMP")))
  
  stats <- localdf |>
    dplyr::select(framework = {{.group_variable}}, value = {{.metric}}) |>
    dplyr::group_by(framework) |>
    dplyr::summarise(
      N = dplyr::n(),
      Mean = mean(value, na.rm = TRUE),
      SD = sd(value, na.rm = TRUE),
      Max = max(value, na.rm = TRUE),
      Min = min(value, na.rm = TRUE),
      .groups = "drop"
    ) |>
    dplyr::arrange(framework) |>
    dplyr::mutate(across(where(is.numeric), \(x) round(x, 2)))
  
  write.csv2(stats, row.names = FALSE, quote = FALSE)
  
  metric_sym <- rlang::ensym(.metric)
  group_variable_sym <- rlang::ensym(.group_variable)
  formula_formatted <- rlang::new_formula(metric_sym, group_variable_sym)
  
  welch_results <- localdf |>
    rstatix::welch_anova_test(formula_formatted) |>
    dplyr::mutate(
      statistic = round(statistic, 2),
      DFd = round(DFd, 2),
      p = format_p(p)
    )
  
  effect_size <- effectsize::omega_squared(oneway.test(formula_formatted, data = localdf, var.equal = FALSE),
                                           partial = FALSE) |>
    dplyr::mutate(
      Omega2 = round(Omega2, 3),
      CI_low = round(CI_low, 3),
      CI_high = round(CI_high, 3)
    )
  
  post_hoc <- localdf |>
    rstatix::games_howell_test(formula_formatted) |>
    dplyr::mutate(
      estimate = round(estimate, 2),
      conf.low = round(conf.low, 2),
      conf.high = round(conf.high, 2),
      p.adj = format_p(p.adj)
    )
  
  cat("\n")
  write.csv2(welch_results, row.names = FALSE, quote = FALSE)
  cat("\n")
  write.csv2(effect_size, row.names = FALSE, quote = FALSE)
  cat("\n")
  write.csv2(post_hoc, row.names = FALSE, quote = FALSE)
  
  invisible(.df)
}

# -------------------------------------------

summary(Measurements)

#4.1 Time to Display
Measurements |>
  .plot_and_save(app_displayed_ms, framework, "41", ) |>
  .descriptive_stats(framework, app_displayed_ms)

#4.2 TTC
Measurements |>
  .plot_and_save(avg_benchmark_ms, framework, "42") |> .descriptive_stats(framework, avg_benchmark_ms)

#4.3 CPU usage
Measurements |>
  .plot_and_save(peak_cpu_usage_perc, framework, "43") |> .descriptive_stats(framework, peak_cpu_usage_perc)

#4.4	Baseline RAM
Measurements |>
  .plot_and_save(baselineRAM_mb, framework, "44") |>  .descriptive_stats(framework, baselineRAM_mb)

#4.5	Benchmark RAM
Measurements |>
  .plot_and_save(benchmarkRAM_mb,
                 framework,
                 "45") |> .descriptive_stats(framework, benchmarkRAM_mb)

# -------------------------------------------

#4.6 Accelerometer stats
Measurements_Accelerometer <- Measurements |>
  dplyr::filter(grepl("accelerometer", benchmark))

summary(Measurements_Accelerometer)

#4.6.1 TTC
Measurements_Accelerometer |>
  .plot_and_save(avg_benchmark_ms,
                 framework,
                 "461",
                 benchmark) |> .descriptive_stats(framework, avg_benchmark_ms)

#4.6.2 CPU usage
Measurements_Accelerometer |>
  .plot_and_save(peak_cpu_usage_perc,
                 framework,
                 "462",
                 benchmark) |> .descriptive_stats(framework, peak_cpu_usage_perc)

#4.6.3 Benchmark RAM
Measurements_Accelerometer |>
  .plot_and_save(
    benchmarkRAM_mb,
    framework,
    "463",
    benchmark
  ) |> .descriptive_stats(framework, benchmarkRAM_mb)


# -------------------------------------------

#4.7 Image Retrieval stats
Measurements_Image <- Measurements |>
  dplyr::filter(grepl("image", benchmark))

summary(Measurements_Image)


#4.7.1 TTC
Measurements_Image |>
  .plot_and_save(avg_benchmark_ms,
                 framework,
                 "471",
                 benchmark) |> .descriptive_stats(framework, avg_benchmark_ms)

#4.7.2 CPU usage
Measurements_Image |>
  .plot_and_save(peak_cpu_usage_perc,
                 framework,
                 "472",
                 benchmark) |> .descriptive_stats(framework, peak_cpu_usage_perc)

#4.7.3 Benchmark RAM
Measurements_Image |>
  .plot_and_save(
    benchmarkRAM_mb,
    framework,
    "473",
    benchmark
  ) |> .descriptive_stats(framework, benchmarkRAM_mb)


# -------------------------------------------

#4.8 Contact creation stats
Measurements_Contacts <- Measurements |>
  dplyr::filter(grepl("contacts", benchmark))

summary(Measurements_Contacts)


#4.8.1 TTC
Measurements_Contacts |>
  .plot_and_save(avg_benchmark_ms,
                 framework,
                 "481",
                 benchmark) |> .descriptive_stats(framework, avg_benchmark_ms)

#4.8.2 CPU usage
Measurements_Contacts |>
  .plot_and_save(peak_cpu_usage_perc,
                 framework,
                 "482",
                 benchmark) |> .descriptive_stats(framework, peak_cpu_usage_perc)

#4.8.3 Benchmark RAM
Measurements_Contacts |>
  .plot_and_save(
    benchmarkRAM_mb,
    framework,
    "483",
    benchmark
  ) |> .descriptive_stats(framework, benchmarkRAM_mb)


# -------------------------------------------

#4.9 Light sensor stats
Measurements_Light <- Measurements |>
  dplyr::filter(grepl("light", benchmark))

summary(Measurements_Light)


#4.9.1 TTC
Measurements_Light |>
  .plot_and_save(avg_benchmark_ms,
                 framework,
                 "491",
                 benchmark) |> .descriptive_stats(framework, avg_benchmark_ms)

#4.9.2 CPU usage
Measurements_Light |>
  .plot_and_save(peak_cpu_usage_perc,
                 framework,
                 "492",
                 benchmark) |> .descriptive_stats(framework, peak_cpu_usage_perc)

#4.9.3 Benchmark RAM
Measurements_Light |>
  .plot_and_save(
    benchmarkRAM_mb,
    framework,
    "493",
    benchmark
  ) |> .descriptive_stats(framework, benchmarkRAM_mb)

# -------------------------------------------

#4.10 Math calculation stats
Measurements_Math <- Measurements |>
  dplyr::filter(grepl("math", benchmark))

summary(Measurements_Math)


#4.10.1 TTC
Measurements_Math |>
  .plot_and_save(avg_benchmark_ms,
                 framework,
                 "4101",
                 benchmark) |> .descriptive_stats(framework, avg_benchmark_ms)

#4.10.2 CPU usage
Measurements_Math |>
  .plot_and_save(peak_cpu_usage_perc,
                 framework,
                 "4102",
                 benchmark) |> .descriptive_stats(framework, peak_cpu_usage_perc)

#4.10.3 Benchmark RAM
Measurements_Math |>
  .plot_and_save(
    benchmarkRAM_mb,
    framework,
    "4103",
    benchmark
  ) |> .descriptive_stats(framework, benchmarkRAM_mb)

# To generate the citations needed grateful is used
# grateful::cite_packages(
#   out.dir = ".",
#   out.format = "docx",
#   citation.style = "ieee",
#   cite.tidyverse = TRUE,
#   
# )
# citation("grateful")
# RStudio.Version()$citation
