document.addEventListener('DOMContentLoaded', function() {

    // Обработчик добавления категории
    document.querySelectorAll('.add-category-btn').forEach(function(btn) {
        btn.addEventListener('click', function() {
            const type = this.dataset.type;
            const inputId = type === 'INCOME' ? 'newIncomeCategory' : 'newExpenseCategory';
            const input = document.getElementById(inputId);
            const name = input.value.trim();
            if (!name) {
                showError('Введите название категории');
                return;
            }

            fetch('/api/categories', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: name, type: type })
            })
                .then(response => {
                    if (response.ok) {
                        location.reload();
                    } else {
                        return response.text().then(text => {
                            let errorMsg = text;
                            try {
                                const json = JSON.parse(text);
                                if (json.error) errorMsg = json.error;
                                else if (json.message) errorMsg = json.message;
                            } catch (e) {
                                // use text as is
                            }
                            throw new Error(errorMsg);
                        });
                    }
                })
                .catch(err => {
                    showError(err.message);
                });
        });
    });

    // Обработчик удаления категории
    document.addEventListener('click', function(e) {
        const deleteBtn = e.target.closest('.delete-category-btn');
        if (!deleteBtn) return;
        const id = deleteBtn.dataset.id;
        if (!id) return;
        if (!confirm('Вы уверены, что хотите удалить эту категорию?')) return;

        fetch('/api/categories/' + id, {
            method: 'DELETE'
        })
            .then(response => {
                if (response.ok) {
                    location.reload();
                } else {
                    return response.text().then(text => {
                        let errorMsg = text;
                        try {
                            const json = JSON.parse(text);
                            if (json.error) errorMsg = json.error;
                            else if (json.message) errorMsg = json.message;
                        } catch (e) {
                            // use text as is
                        }
                        throw new Error(errorMsg);
                    });
                }
            })
            .catch(err => {
                showError(err.message);
            });
    });

    // Обработчик редактирования категории
    document.addEventListener('click', function(e) {
        const editBtn = e.target.closest('.edit-category-btn');
        if (!editBtn) return;
        const listItem = editBtn.closest('.list-group-item');
        const nameSpan = listItem.querySelector('.category-name');
        const currentName = nameSpan.textContent.trim();
        const id = editBtn.dataset.id;

        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'form-control form-control-sm edit-input';
        input.value = currentName;
        input.dataset.originalName = currentName;
        input.style.display = 'inline-block';
        input.style.width = 'auto';

        nameSpan.replaceWith(input);
        input.focus();
        input.select();

        function saveCategory() {
            const newName = input.value.trim();
            const originalName = input.dataset.originalName;

            if (newName === originalName) {
                restoreSpan(originalName);
                return;
            }

            if (!newName) {
                showError('Название категории не может быть пустым');
                restoreSpan(originalName);
                return;
            }

            fetch('/api/categories/' + id, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: newName })
            })
                .then(response => {
                    if (response.ok) {
                        location.reload();
                    } else {
                        return response.text().then(text => {
                            let errorMsg = text;
                            try {
                                const json = JSON.parse(text);
                                if (json.error) errorMsg = json.error;
                                else if (json.message) errorMsg = json.message;
                            } catch (e) {
                                // use text as is
                            }
                            throw new Error(errorMsg);
                        });
                    }
                })
                .catch(err => {
                    showError(err.message);
                    restoreSpan(originalName);
                });
        }

        function restoreSpan(text) {
            const span = document.createElement('span');
            span.className = 'category-name';
            span.textContent = text;
            input.replaceWith(span);
        }

        input.addEventListener('blur', saveCategory);
        input.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                input.blur();
            }
            if (e.key === 'Escape') {
                input.value = currentName;
                input.blur();
            }
        });
    });

    // Функции для показа сообщений
    function showError(message) {
        const alert = document.getElementById('errorAlert');
        if (alert) {
            alert.textContent = message;
            alert.style.display = 'block';
            setTimeout(() => {
                alert.style.display = 'none';
            }, 5000);
        } else {
            alert('Ошибка: ' + message);
        }
    }

    function showSuccess(message) {
        const alert = document.getElementById('successAlert');
        if (!alert) return;
        alert.textContent = message;
        alert.style.display = 'block';
        setTimeout(() => { alert.style.display = 'none'; }, 3000);
    }

    window.showError = showError;
    window.showSuccess = showSuccess;
});