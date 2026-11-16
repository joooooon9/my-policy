/* "더 보기" on the notice list: fetches the next page and appends its new cards in place, so the page
   keeps its scroll. The link alone also works without script (it reloads with the next page and jumps to it).
   The address keeps ?page=N, so back from a detail page shows the same cards. */
(function () {
  document.addEventListener('click', function (event) {
    var link = event.target.closest && event.target.closest('[data-more]');
    if (!link || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey) return;
    var list = document.querySelector('.results .cards');
    var holder = link.closest('.more');
    if (!list || !holder || !window.fetch || !window.DOMParser) return;
    event.preventDefault();
    if (link.getAttribute('aria-busy') === 'true') return;
    link.setAttribute('aria-busy', 'true');

    var url = link.href.split('#')[0];
    fetch(url, { headers: { 'Accept': 'text/html' } })
      .then(function (res) { if (!res.ok) throw new Error(res.status); return res.text(); })
      .then(function (html) {
        var doc = new DOMParser().parseFromString(html, 'text/html');
        var items = doc.querySelectorAll('.results .cards > li');
        var have = list.children.length;
        var first = null;
        for (var i = have; i < items.length; i++) {
          var item = document.importNode(items[i], true);
          list.appendChild(item);
          if (!first) first = item;
        }
        var next = doc.querySelector('.results .more');
        if (next) holder.replaceWith(document.importNode(next, true)); else holder.remove();
        if (window.history && history.replaceState) history.replaceState(history.state, '', url);
        // keyboard and screen reader users land on the first new card
        var target = first && first.querySelector('a');
        if (target) target.focus({ preventScroll: true });
      })
      .catch(function () {
        window.location.href = link.href; // fall back to the plain link
      });
  });
})();
