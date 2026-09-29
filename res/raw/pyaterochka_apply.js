(function (names) {
    var norm = function (s) { return (s || '').toLowerCase().replace(/\s+/g, ' ').trim(); };
    var buttons = Array.from(document.querySelectorAll('button,[role="button"]'));
    var count = 0;
    buttons.forEach(function (button) {
        if (!/\d+(?:[.,]\d+)?%\s*$/.test(button.innerText.trim())) return;
        var label = button.innerText.replace(/\s+/g, ' ').trim().replace(/\s+И на товары со скидкой.*$/i, '').replace(/\s+\d+(?:[.,]\d+)?%$/, '');
        var wanted = names.some(function (name) { return norm(label) === norm(name); });
        var parent = button.parentElement, box = null;
        for (var i = 0; parent && i < 4; i++, parent = parent.parentElement) {
            var boxes = parent.querySelectorAll('input[type="checkbox"]');
            if (boxes.length === 1) { box = boxes[0]; break; }
            if (boxes.length > 1) break;
        }
        if (box && box.checked !== wanted && !box.disabled) { box.click(); count++; }
    });
    if (count) setTimeout(function () {
        var save = Array.from(document.querySelectorAll('button')).find(function (b) {
            return !b.disabled && /^(Выбрать(?:\s+\d+)?|Сохранить(?: выбор)?|Подтвердить)$/.test(b.innerText.trim());
        });
        if (save) save.click();
    }, 700);
    return JSON.stringify({count: count});
})(__NAMES__)
