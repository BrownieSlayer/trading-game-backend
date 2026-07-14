package app.dto.market;

/** Une actualité financière générale (marché large, pas liée à un ticker précis). */
public record NewsItem(String title, String source, String date, String url) {}
