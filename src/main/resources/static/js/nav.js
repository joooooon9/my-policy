/* The menu button of the search and detail screens, the same as /about's (assets/system.js nav()):
   1023px and below the words fold into a full-screen sheet; Escape, a link tap or a wider screen closes it. */
(function () {
  var header = document.querySelector('.site-header');
  if (!header) return;
  var toggle = header.querySelector('.site-header__toggle');
  if (!toggle) return;
  var bars = '<svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true"><path d="M2 5h14M2 13h14" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>';
  var cross = '<svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true"><path d="M4 4l10 10M14 4L4 14" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>';

  function setOpen(open) {
    header.classList.toggle('is-open', open);
    document.body.classList.toggle('nav-locked', open);
    toggle.setAttribute('aria-expanded', String(open));
    toggle.setAttribute('aria-label', open ? '메뉴 닫기' : '메뉴 열기');
    toggle.innerHTML = open ? cross : bars;
  }

  toggle.addEventListener('click', function () { setOpen(!header.classList.contains('is-open')); });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && header.classList.contains('is-open')) { setOpen(false); toggle.focus(); }
  });
  header.addEventListener('click', function (e) {
    if (header.classList.contains('is-open') && e.target.closest && e.target.closest('a[href]')) setOpen(false);
  });
  if (window.matchMedia) {
    var wide = window.matchMedia('(min-width: 1024px)');
    var close = function (e) { if (e.matches) setOpen(false); };
    if (wide.addEventListener) wide.addEventListener('change', close); else if (wide.addListener) wide.addListener(close);
  }
})();
