document.addEventListener('DOMContentLoaded', function() {

    // ----- Фильтрация категорий -----
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

    // ----- Форма добавления -----
    var addTypeSelect = document.getElementById('addType');
    var addCategorySelect = document.getElementById('addCategory');

    function updateAddCategories() {
        var selectedType = addTypeSelect.value;
        var filtered = filterCategoriesByType(selectedType);
        populateCategorySelect(addCategorySelect, filtered);
        if (selectedCategoryId) {
            addCategorySelect.value = selectedCategoryId;
        }
    }
    updateAddCategories();
    addTypeSelect.addEventListener('change', function() {
        updateAddCategories();
        var currentVal = addCategorySelect.value;
        if (currentVal) {
            var cat = categories.find(c => c.id === currentVal);
            if (cat && cat.type !== this.value) {
                addCategorySelect.value = '';
            }
        }
    });

    // ----- Редактирование -----
    function formatDateForInput(dateStr) {
        if (!dateStr) return '';
        // Если строка содержит 'Z' (UTC), обрезаем до 16 символов
        if (dateStr.includes('Z')) {
            return dateStr.substring(0, 16);
        }
        // Если строка уже в формате YYYY-MM-DDTHH:mm:ss, берём первые 16 символов
        if (dateStr.includes('T')) {
            return dateStr.substring(0, 16);
        }
        // Иначе парсим как Date
        const d = new Date(dateStr);
        if (isNaN(d)) return '';
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        return `${year}-${month}-${day}T${hours}:${minutes}`;
    }

    function enableEditing(row) {
        const id = row.dataset.id;
        const transaction = transactionsData.find(t => t.id === id);
        if (!transaction) {
            alert('Транзакция не найдена');
            return;
        }

        const editRow = document.getElementById('editTemplate').cloneNode(true);
        editRow.style.display = '';
        editRow.id = 'editRow-' + id;
        const saveBtn = editRow.querySelector('.save-edit-btn');
        saveBtn.dataset.id = id;
        editRow.querySelector('.cancel-edit-btn').dataset.id = id;

        editRow.querySelector('.edit-amount').value = transaction.amount || '';
        const typeSelect = editRow.querySelector('.edit-type');
        typeSelect.value = transaction.type || 'INCOME';
        const categorySelect = editRow.querySelector('.edit-category');
        const filtered = filterCategoriesByType(transaction.type);
        populateCategorySelect(categorySelect, filtered);
        Array.from(categorySelect.options).forEach(opt => {
            if (opt.text === transaction.categoryName) {
                opt.selected = true;
            }
        });
        const dateInput = editRow.querySelector('.edit-date');
        dateInput.value = transaction.date ? formatDateForInput(transaction.date) : '';
        editRow.querySelector('.edit-description').value = transaction.description || '';

        // При изменении типа обновляем категории
        typeSelect.addEventListener('change', function() {
            const newType = this.value;
            const filteredNew = filterCategoriesByType(newType);
            const catSelect = this.closest('tr').querySelector('.edit-category');
            populateCategorySelect(catSelect, filteredNew);
            const currentCat = catSelect.value;
            if (currentCat) {
                const cat = categories.find(c => c.id === currentCat);
                if (cat && cat.type !== newType) {
                    catSelect.value = '';
                }
            }
        });

        row.replaceWith(editRow);
    }

    // ----- Обработчики -----
    document.addEventListener('click', function(e) {
        const editBtn = e.target.closest('.edit-btn');
        if (!editBtn) return;
        const row = editBtn.closest('.transaction-row');
        if (!row) return;
        const id = row.dataset.id;
        if (!id) {
            alert('ID транзакции не найден');
            return;
        }
        enableEditing(row);
    });

    // Сохранить
    document.addEventListener('click', function(e) {
        const saveBtn = e.target.closest('.save-edit-btn');
        if (!saveBtn) return;
        const editRow = saveBtn.closest('tr');
        const id = saveBtn.dataset.id;

        // Удаляем предыдущее сообщение об ошибке
        const errorDiv = editRow.querySelector('.edit-error');
        if (errorDiv) {
            errorDiv.textContent = '';
            errorDiv.style.display = 'none';
        }

        const amount = editRow.querySelector('.edit-amount').value;
        const type = editRow.querySelector('.edit-type').value;
        const categoryId = editRow.querySelector('.edit-category').value;
        const dateVal = editRow.querySelector('.edit-date').value;
        const description = editRow.querySelector('.edit-description').value;

        const payload = {
            amount: parseFloat(amount),
            type: type,
            categoryId: categoryId || null,
            date: dateVal ? dateVal : null,
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
                    return response.text().then(text => {
                        let errorMsg = 'Ошибка при обновлении';
                        try {
                            const json = JSON.parse(text);
                            errorMsg = json.error || json.message || errorMsg;
                        } catch (e) {
                            errorMsg = text || errorMsg;
                        }
                        throw new Error(errorMsg);
                    });
                }
            })
            .catch(err => {
                if (errorDiv) {
                    errorDiv.textContent = err.message;
                    errorDiv.style.display = 'block';
                } else {
                    // fallback – alert, если вдруг нет блока
                    alert(err.message);
                }
            });
    });

    // Отмена
    document.addEventListener('click', function(e) {
        const cancelBtn = e.target.closest('.cancel-edit-btn');
        if (!cancelBtn) return;
        location.reload();
    });

    // Удалить
    document.addEventListener('click', function(e) {
        const deleteBtn = e.target.closest('.delete-btn');
        if (!deleteBtn) return;
        const id = deleteBtn.dataset.id;
        if (!id) {
            alert('ID транзакции не найден');
            return;
        }
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
    });

});