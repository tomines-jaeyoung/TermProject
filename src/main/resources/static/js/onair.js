// 온에어마켓 화면 보조 스크립트: 남은 시간 표시, 등록 폼(파일 크기 사전 검사·업로드 진행률·기간 입력 표시)
(function () {
    function remaining(ends) {
        var left = Math.max(0, Math.floor((ends - Date.now()) / 1000));
        if (left === 0) return '종료';
        var d = Math.floor(left / 86400), h = Math.floor(left % 86400 / 3600),
            m = Math.floor(left % 3600 / 60), s = left % 60;
        if (d > 0) return d + '일 ' + h + '시간';
        function p(n) { return (n < 10 ? '0' : '') + n; }
        return p(h) + ':' + p(m) + ':' + p(s);
    }

    function tick() {
        document.querySelectorAll('.oa-countdown[data-ends]').forEach(function (el) {
            el.textContent = remaining(Number(el.getAttribute('data-ends')));
        });
    }

    function setupSellForm(form) {
        var hint = document.getElementById('file-hint');
        // 시작 시각 / 종료 시각 입력칸은 해당 옵션을 골랐을 때만 표시
        function toggle() {
            var start = form.querySelector('input[name=startMode]:checked');
            var dur = form.querySelector('input[name=duration]:checked');
            form.querySelectorAll('[data-show-when]').forEach(function (el) {
                var w = el.getAttribute('data-show-when');
                el.hidden = !((start && start.value === w) || (dur && dur.value === w));
            });
        }
        form.addEventListener('change', toggle);
        toggle();

        // 서버에서도 다시 검사하지만, 너무 큰 파일은 보내기 전에 안내
        function checkFiles() {
            var ok = true, msgs = [];
            form.querySelectorAll('input[type=file]').forEach(function (input) {
                var f = input.files && input.files[0];
                if (f && f.size > Number(input.getAttribute('data-max'))) {
                    ok = false;
                    msgs.push(input.getAttribute('data-kind') + ' 파일이 너무 큽니다 (' + (f.size / 1048576).toFixed(1) + 'MB).');
                }
            });
            hint.textContent = msgs.join(' ');
            hint.style.color = ok ? '' : '#c0392b';
            return ok;
        }
        form.addEventListener('change', checkFiles);

        // 업로드 진행률 표시: XMLHttpRequest 로 보내고, 끝나면 서버가 돌려준 화면으로 이동
        form.addEventListener('submit', function (e) {
            if (!checkFiles()) { e.preventDefault(); return; }
            if (!window.FormData || !window.XMLHttpRequest) return;
            e.preventDefault();
            var box = form.querySelector('.oa-progress'), bar = box.querySelector('.oa-progress-bar'), label = box.querySelector('span');
            var button = form.querySelector('button[type=submit]');
            box.hidden = false; button.disabled = true; button.textContent = '업로드 중…';
            var xhr = new XMLHttpRequest();
            xhr.open('POST', form.action);
            xhr.upload.onprogress = function (ev) {
                if (!ev.lengthComputable) return;
                var pct = Math.round(ev.loaded / ev.total * 100);
                bar.style.width = pct + '%'; label.textContent = pct + '%';
            };
            xhr.onload = function () {
                if (xhr.status >= 400) {                       // 서버 오류(로그인 만료 403, 용량 초과 413 등): 원문 대신 안내
                    box.hidden = true; button.disabled = false; button.textContent = '경매 등록';
                    hint.textContent = xhr.status === 403
                        ? '로그인이 만료되었거나 요청이 만료되었습니다. 새로고침한 뒤 다시 시도해 주세요.'
                        : (xhr.status === 413 ? '파일이 너무 큽니다. 영상은 50MB, 사진은 8MB까지 올릴 수 있습니다.'
                        : '등록에 실패했습니다 (오류 ' + xhr.status + '). 잠시 후 다시 시도해 주세요.');
                    hint.style.color = '#c0392b';
                    return;
                }
                if (xhr.responseURL && xhr.responseURL.indexOf('/sell/timed') === -1) {
                    window.location.href = xhr.responseURL;      // 등록 성공 → 상세 화면
                } else {
                    document.open(); document.write(xhr.responseText); document.close();   // 입력 오류 화면
                }
            };
            xhr.onerror = function () {
                button.disabled = false; button.textContent = '경매 등록';
                hint.textContent = '업로드에 실패했습니다. 연결을 확인하고 다시 시도해 주세요.';
                hint.style.color = '#c0392b';
            };
            xhr.send(new FormData(form));
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        tick();
        setInterval(tick, 1000);
        var form = document.getElementById('sell-form');
        if (form) setupSellForm(form);
    });
})();
