document.addEventListener('DOMContentLoaded', function() {
    fetch('/api/analytics/metrics')
        .then(response => response.json())
        .then(data => {
            const ctx = document.getElementById('chart').getContext('2d');
            new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: ['Доходы', 'Расходы'],
                    datasets: [{
                        label: 'Сумма',
                        data: [data.income, data.expense],
                        backgroundColor: ['#4caf50', '#f44336']
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: { display: false }
                    }
                }
            });
        });
});