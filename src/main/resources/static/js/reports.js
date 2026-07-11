(function() {
    // Заполнение выбора года (текущий и 5 лет назад)
    const yearSelect = document.getElementById('year');
    const currentYear = new Date().getFullYear();
    for (let y = currentYear; y >= currentYear - 5; y--) {
        const opt = document.createElement('option');
        opt.value = y;
        opt.text = y;
        yearSelect.appendChild(opt);
    }
    yearSelect.value = currentYear;

    // Переключение между полями периода
    const periodSelect = document.getElementById('period');
    const monthFields = document.getElementById('monthFields');
    const quarterFields = document.getElementById('quarterFields');

    function updateFields() {
        const period = periodSelect.value;
        monthFields.style.display = (period === 'MONTH') ? 'block' : 'none';
        quarterFields.style.display = (period === 'QUARTER') ? 'block' : 'none';
    }
    periodSelect.addEventListener('change', updateFields);
    updateFields();

    function pad(n) {
        return String(n).padStart(2, '0');
    }

    function formatLocalDateTime(date) {
        const year = date.getFullYear();
        const month = pad(date.getMonth() + 1);
        const day = pad(date.getDate());
        const hours = pad(date.getHours());
        const minutes = pad(date.getMinutes());
        const seconds = pad(date.getSeconds());
        return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
    }

    function formatDateForFilename(dateStr) {
        const parts = dateStr.split('T')[0].split('-');
        return `${parts[2]}-${parts[1]}-${parts[0]}`;
    }

    // Обработчик генерации
    document.getElementById('generateBtn').addEventListener('click', function() {
        const period = periodSelect.value;
        let month = null, quarter = null;
        if (period === 'MONTH') {
            month = parseInt(document.getElementById('month').value);
        } else if (period === 'QUARTER') {
            quarter = parseInt(document.getElementById('quarter').value);
        }
        const year = parseInt(document.getElementById('year').value);
        const format = document.getElementById('format').value;

        let from, to;
        if (period === 'MONTH') {
            from = new Date(year, month - 1, 1, 0, 0, 0);
            to = new Date(year, month, 0, 23, 59, 59);
        } else if (period === 'QUARTER') {
            const startMonth = (quarter - 1) * 3;
            from = new Date(year, startMonth, 1, 0, 0, 0);
            to = new Date(year, startMonth + 3, 0, 23, 59, 59);
        } else { // YEAR
            from = new Date(year, 0, 1, 0, 0, 0);
            to = new Date(year, 11, 31, 23, 59, 59);
        }

        // Форматируем даты в локальном времени (без UTC-смещения)
        const fromStr = formatLocalDateTime(from);
        const toStr = formatLocalDateTime(to);

        const url = '/api/reports/export?period=' + period +
            '&from=' + encodeURIComponent(fromStr) +
            '&to=' + encodeURIComponent(toStr) +
            '&format=' + format;

        const errorAlert = document.getElementById('errorAlert');
        errorAlert.style.display = 'none';

        fetch(url, {
            method: 'GET'
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => {
                        throw new Error(err.error || 'Ошибка при генерации отчёта');
                    });
                }
                return response.blob();
            })
            .then(blob => {
                const link = document.createElement('a');
                link.href = URL.createObjectURL(blob);
                // Формируем имя файла: financialReport_<от>_<до>.<расширение>
                const fromPart = formatDateForFilename(fromStr);
                const toPart = formatDateForFilename(toStr);
                const ext = format === 'PDF' ? 'pdf' : 'xlsx';
                link.download = `financialReport_${fromPart}_${toPart}.${ext}`;
                document.body.appendChild(link);
                link.click();
                link.remove();
                URL.revokeObjectURL(link.href);
            })
            .catch(err => {
                errorAlert.textContent = err.message;
                errorAlert.style.display = 'block';
            });
    });
})();