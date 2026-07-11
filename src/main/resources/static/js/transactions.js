document.addEventListener('DOMContentLoaded', function() {

    // ----- Вспомогательные функции -----

    function filterCategoriesByType(type) {
        return categories.filter(c => c.type === type);
    }

    function populateCategorySelect(selectElement, categoryList) {
        selectElement.innerHTML = '';
        var emptyOpt = document.createElement('option');
        emptyOpt.value = '';
        emptyOpt.text = '— Без категории —';
        selectElement.appendChild(emptyOpt);
        categoryList.forEach(c => {
            var opt = document.createElement('option');
            opt.value = c.id;
            opt.text = c.name;
            selectElement.appendChild(opt);
        });
    }

    function formatDateForInput(dateStr) {
        if (!dateStr) return '';
        var date = new Date(dateStr);
        if (isNaN(date)) return '';
        var year = date.getFullYear();
        var month = String(date.getMonth() + 1).padStart(2, '0');
        var day = String(date.getDate()).padStart(2, '0');
        var hours = String(date.getHours()).padStart(2, '0');
        var minutes = String(date.getMinutes()).padStart(2, '0');
        return year + '-' + month + '-' + day + 'T' + hours + ':' + minutes;
    }

    function getTypeFromText(typeText) {
        return typeText === 'Доход' ? 'INCOME' : 'EXPENSE';
    }

    // ----- Форма добавления -----

    var addTypeSelect = document.getElementById('addType');
    var addCategorySelect = document.getElementById('addCategory');

    function updateAddCategories() {
        var selectedType = addTypeSelect.value;
        var filtered = filterCategoriesByType(selectedType);
        populateCategorySelect(addCategorySelect, filtered);
        // Устанавливаем выбранную категорию из модели (если есть)
        if (selectedCategoryId) {
            addCategorySelect.value = selectedCategoryId;
            // Проверяем, что выбранная категория соответствует типу
            var cat = categories.find(c => c.id === selectedCategoryId);
            if (cat && cat.type !== selectedType) {
                addCategorySelect.value = '';
            }
        }
    }

    // Инициализация
    updateAddCategories();

    addTypeSelect.addEventListener('change', function() {
        updateAddCategories();
        // Если выбранная категория не подходит под новый тип, сброс
        var currentVal = addCategorySelect.value;
        if (currentVal) {
            var cat = categories.find(c => c.id === currentVal);
            if (cat && cat.type !== this.value) {
                addCategorySelect.value = '';
            }
        }
    });

    // ----- Inline редактирование -----

    function enableEditing(row) {
        const id = row.dataset.id;
        const editRow = document.getElementById('editTemplate').cloneNode(true);
        editRow.style.display = '';
        editRow.id = 'editRow-' + id;
        editRow.querySelector('.save-edit-btn').dataset.id = id;
        editRow.querySelector('.cancel-edit-btn').dataset.id = id;

        // Чтение текущих значений
        const displayDate = row.querySelector('.display-date').textContent.trim();
        const displayCategory = row.querySelector('.display-category').textContent.trim();
        const displayTypeText = row.querySelector('.display-type').textContent.trim(); // "Доход" или "Расход"
        const displayAmount = row.querySelector('.display-amount').textContent.trim();
        const displayDescription = row.querySelector('.display-description').textContent.trim();

        // Тип в INCOME/EXPENSE
        var typeValue = getTypeFromText(displayTypeText);

        // Устанавливаем значения
        var dateInput = editRow.querySelector('.edit-date');
        var formattedDate = formatDateForInput(displayDate);
        dateInput.value = formattedDate || '';

        var amountInput = editRow.querySelector('.edit-amount');
        amountInput.value = displayAmount;

        var descInput = editRow.querySelector('.edit-description');
        descInput.value = displayDescription;

        var typeSelect = editRow.querySelector('.edit-type');
        typeSelect.value = typeValue;

        // Заполняем категории по типу
        var categorySelect = editRow.querySelector('.edit-category');
        var filtered = filterCategoriesByType(typeValue);
        populateCategorySelect(categorySelect, filtered);

        // Устанавливаем выбранную категорию по имени
        Array.from(categorySelect.options).forEach(opt => {
            if (opt.text === displayCategory) {
                opt.selected = true;
            }
        });

        // Слушатель изменения типа в редактируемой строке
        typeSelect.addEventListener('change', function() {
            var newType = this.value;
            var filteredNew = filterCategoriesByType(newType);
            var catSelect = this.closest('tr').querySelector('.edit-category');
            populateCategorySelect(catSelect, filteredNew);
            // Если выбранная категория не соответствует новому типу, сбрасываем
            var currentCat = catSelect.value;
            if (currentCat) {
                var cat = categories.find(c => c.id === currentCat);
                if (cat && cat.type !== newType) {
                    catSelect.value = '';
                }
            }
        });

        // Заменяем строку
        row.replaceWith(editRow);
    }

    // ----- Обработчики -----

    // Редактирование
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('edit-btn')) {
            const row = e.target.closest('.transaction-row');
            if (row) {
                enableEditing(row);
            }
        }
    });

    // Сохранение
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('save-edit-btn')) {
            const editRow = e.target.closest('tr');
            const id = e.target.dataset.id;

            const dateVal = editRow.querySelector('.edit-date').value;
            const categoryId = editRow.querySelector('.edit-category').value;
            const type = editRow.querySelector('.edit-type').value;
            const amount = editRow.querySelector('.edit-amount').value;
            const description = editRow.querySelector('.edit-description').value;

            const payload = {
                amount: parseFloat(amount),
                type: type,
                categoryId: categoryId || null,
                date: dateVal ? new Date(dateVal).toISOString() : null,
                description: description
            };

            fetch('/api/transactions/' + id, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            })
                .then(response => {
                    if (response.ok) {
                        location.reload();
                    } else {
                        response.json().then(err => alert('Ошибка: ' + err.error));
                    }
                })
                .catch(err => alert('Ошибка: ' + err));
        }
    });

    // Отмена
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('cancel-edit-btn')) {
            location.reload();
        }
    });

    // Удаление
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('delete-btn')) {
            const id = e.target.dataset.id;
            if (confirm('Вы уверены, что хотите удалить эту транзакцию?')) {
                fetch('/api/transactions/' + id, { method: 'DELETE' })
                    .then(response => {
                        if (response.ok) {
                            location.reload();
                        } else {
                            alert('Ошибка при удалении');
                        }
                    });
            }
        }
    });

});