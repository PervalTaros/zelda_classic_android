// Appliqué à la page de jeu.

// 1) Réglage « commandes tactiles » choisi dans le menu.
(function () {
  try {
    if (localStorage.getItem('zc_touch') === '0') {
      document.documentElement.classList.add('zc-no-touch');
    }
  } catch (e) { /* stockage indisponible : on garde le comportement par défaut */ }
})();

// 2) Son : le navigateur suspend l'audio tant qu'il n'y a pas eu de geste. On repère les contextes
//    audio créés par le moteur et on les relance au premier toucher / à la première touche.
(function () {
  var Orig = window.AudioContext || window.webkitAudioContext;
  if (!Orig) return;
  var ctxs = [];
  function Patched() {
    var args = [null].concat(Array.prototype.slice.call(arguments));
    var c = new (Function.prototype.bind.apply(Orig, args))();
    ctxs.push(c);
    return c;
  }
  Patched.prototype = Orig.prototype;
  window.AudioContext = Patched;
  if (window.webkitAudioContext) window.webkitAudioContext = Patched;
  function resume() {
    ctxs.forEach(function (c) {
      if (c.state === 'suspended') { try { c.resume(); } catch (e) {} }
    });
  }
  ['touchstart', 'touchend', 'pointerdown', 'mousedown', 'keydown', 'click'].forEach(function (ev) {
    window.addEventListener(ev, resume, { capture: true, passive: true });
  });
  window.addEventListener('gamepadconnected', resume);
})();
