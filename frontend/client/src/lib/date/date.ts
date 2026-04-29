export const formatDateLocal = (
    date: string | Date,
    locale: string | 'en-GB' | 'ro-RO' | 'ru-RU' = 'ro-RO'
): string => {
    const d = typeof date === 'string' ? new Date(date) : date;
    if (isNaN(d.getTime())) return '';

    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();

    // ro-RO & ru-RU → 10.02.2026
    if (locale === 'ro-RO' || locale === 'ru-RU') {
        return `${day}.${month}.${year}`;
    }

    // en-GB → 10/02/2026
    return `${day}/${month}/${year}`;
};