// Appliqué à la page de jeu : réglage « commandes tactiles » choisi dans le menu.
(function () {
  try {
    if (localStorage.getItem('zc_touch') === '0') {
      document.documentElement.classList.add('zc-no-touch');
    }
  } catch (e) { /* stockage indisponible : on garde le comportement par défaut */ }
})();
