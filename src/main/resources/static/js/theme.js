/* Light and dark screens: follows the system setting until the visitor picks one with the
   header button ([data-theme-toggle]); the pick is kept in this browser only (localStorage).
   Loaded in <head> without defer so the right colours apply before the first paint. */
(function () {
  var KEY = 'theme';
  var root = document.documentElement;
  var media = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;
  var saved = null;
  try { saved = localStorage.getItem(KEY); } catch (e) { /* storage blocked: follow the system */ }
  if (saved === 'dark' || saved === 'light') root.setAttribute('data-theme', saved);

  function current() {
    var t = root.getAttribute('data-theme');
    if (t === 'dark' || t === 'light') return t;
    return media && media.matches ? 'dark' : 'light';
  }

  function sync() {
    var dark = current() === 'dark';
    var buttons = document.querySelectorAll('[data-theme-toggle]');
    for (var i = 0; i < buttons.length; i++) buttons[i].setAttribute('aria-pressed', String(dark));
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', dark ? '#17171c' : '#ffffff');
  }

  document.addEventListener('click', function (event) {
    var button = event.target.closest && event.target.closest('[data-theme-toggle]');
    if (!button) return;
    var next = current() === 'dark' ? 'light' : 'dark';
    root.setAttribute('data-theme', next);
    try { localStorage.setItem(KEY, next); } catch (e) { /* this visit only */ }
    sync();
  });
  if (media && media.addEventListener) media.addEventListener('change', sync);
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', sync); else sync();
})();
