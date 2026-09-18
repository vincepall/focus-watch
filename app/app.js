// RT Focus & Film Calculator - Core Application for Google Pixel Watch 3
// Supports Wear OS 3 / 4 / 5, rotary crown navigation, and standalone offline execution.

(function() {
  'use strict';

  // --- STATE ---
  const state = {
    mode: 'time', // 'time', 'mamin', 'ci'
    
    // Time mode
    minutes: 2,
    seconds: 0,
    
    // mA*min mode
    mamin: 8.0,
    ma: 3.0,
    
    // Ci mode
    ciOldMin: 1,
    ciOldSec: 30,
    ciOldVal: 50.0,
    ciNewVal: 40.0,
    ciSelectedSourceIdx: -1,
    
    // FFD
    ffdChanged: true,
    ffdOld: 80,
    ffdNew: 100,
    
    // Film
    filmChanged: false,
    filmStart: 'D4', // 'D4', 'D5', 'D7'
    
    // Material & kV
    matChanged: false,
    selectedKv: '300', // '150', '200', '250', '300'
    selectedMatIdx: -1,
    manualFactor: 1.0,
    
    // Timer state
    timerActive: false,
    timerTotalSeconds: 0,
    timerRemainingSeconds: 0,
    timerInterval: null,
    timerPaused: false
  };

  const STORAGE_KEY = 'pixelwatch_rt_focus_presets_v1';

  // --- HAPTIC FEEDBACK ---
  function haptic(pattern = 15) {
    try {
      if (window.AndroidWatch && typeof window.AndroidWatch.vibrate === 'function') {
        window.AndroidWatch.vibrate(Array.isArray(pattern) ? pattern[0] : pattern);
      } else if (navigator.vibrate) {
        navigator.vibrate(pattern);
      }
    } catch (e) {
      // Ignore vibration errors
    }
  }

  // --- SOUND BEEP ---
  function playBeep() {
    try {
      const ctx = new (window.AudioContext || window.webkitAudioContext)();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(880, ctx.currentTime); // A5
      gain.gain.setValueAtTime(0.3, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.4);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.4);
    } catch(e) {}
  }

  // --- CLOCK ---
  function updateClock() {
    const clockEl = document.getElementById('watch-clock');
    if (!clockEl) return;
    const now = new Date();
    const hrs = String(now.getHours()).padStart(2, '0');
    const mins = String(now.getMinutes()).padStart(2, '0');
    clockEl.textContent = `${hrs}:${mins}`;
  }

  // --- ISOTOPE DECAY CALCULATION ---
  function calculateCurrentCi(initialCi, type, dateStr) {
    if (!dateStr) return initialCi;
    const parts = dateStr.split('-');
    const srcDate = new Date(parts[2], parts[1] - 1, parts[0]);
    const today = new Date();
    const daysDiff = (today - srcDate) / (1000 * 60 * 60 * 24);
    const halfLife = (type === 'Se75') ? 120 : 74;
    return initialCi / Math.pow(2, daysDiff / halfLife);
  }

  // --- MATERIAL FACTOR HELPER ---
  function getMaterialFactor(mat, kv) {
    if (!mat) return 1.0;
    if (kv === '150') return mat.f150;
    if (kv === '200') return mat.f200;
    if (kv === '250') return mat.f250;
    return mat.f300;
  }

  function getEffectiveMaterialFactor() {
    if (!state.matChanged) return 1.0;
    if (state.selectedMatIdx >= 0 && typeof MATERIALS_DATA !== 'undefined' && MATERIALS_DATA[state.selectedMatIdx]) {
      return getMaterialFactor(MATERIALS_DATA[state.selectedMatIdx], state.selectedKv);
    }
    return state.manualFactor || 1.0;
  }

  // --- FILM CONVERSIONS ---
  function convertD4(d4) {
    return { d4: d4, d5: d4 / 1.6666, d7: d4 / 2.44 };
  }
  function convertD5(d5) {
    const d4 = d5 * 1.6666;
    return { d4: d4, d5: d5, d7: d4 / 2.44 };
  }
  function convertD7(d7) {
    const d4 = d7 * 2.44;
    return { d4: d4, d5: d4 / 1.6666, d7: d7 };
  }

  // --- TIME FORMATTING ---
  function formatTime(seconds) {
    const s = Math.max(0, Math.round(seconds));
    const m = Math.floor(s / 60);
    const remS = s % 60;
    const sStr = remS < 10 ? '0' + remS : String(remS);
    if (m > 0) {
      return `${m}m ${sStr}s`;
    }
    return `${sStr}s`;
  }

  function formatTimerDigits(totalSec) {
    const s = Math.max(0, Math.round(totalSec));
    const m = Math.floor(s / 60);
    const remS = s % 60;
    return `${String(m).padStart(2, '0')}:${String(remS).padStart(2, '0')}`;
  }

  // --- MAIN CALCULATION ENGINE ---
  function calculate() {
    let baseTimeSec = 0;

    if (state.mode === 'time') {
      baseTimeSec = (state.minutes * 60) + state.seconds;
    } else if (state.mode === 'mamin') {
      if (state.ma > 0) {
        baseTimeSec = (state.mamin / state.ma) * 60;
      }
    } else if (state.mode === 'ci') {
      const oldSec = (state.ciOldMin * 60) + state.ciOldSec;
      if (state.ciNewVal > 0 && state.ciOldVal > 0) {
        baseTimeSec = oldSec * (state.ciOldVal / state.ciNewVal);
      }
    }

    if (baseTimeSec <= 0) baseTimeSec = 0;

    // 1. FFD Inverse Square Law: t2 = t1 * (FFD2 / FFD1)^2
    let calcTime = baseTimeSec;
    if (state.ffdChanged && state.ffdOld > 0 && state.ffdNew > 0) {
      calcTime = baseTimeSec * Math.pow(state.ffdNew / state.ffdOld, 2);
    }

    // 2. Material Factor
    const factor = getEffectiveMaterialFactor();
    calcTime = calcTime * factor;

    // Render result
    renderResult(calcTime, baseTimeSec, factor);
    return calcTime;
  }

  // --- RENDER RESULT ---
  function renderResult(calcTime, baseTimeSec, factor) {
    const mainTimeEl = document.getElementById('result-main-time');
    const subtextEl = document.getElementById('result-subtext');
    const filmsGridEl = document.getElementById('converted-films-grid');
    const timerBtn = document.getElementById('btn-open-timer');

    if (!mainTimeEl) return;

    const roundedSec = Math.round(calcTime);

    if (state.filmChanged) {
      // Show film conversions
      mainTimeEl.style.display = 'none';
      filmsGridEl.style.display = 'flex';
      filmsGridEl.innerHTML = '';

      let converted = {};
      if (state.filmStart === 'D4') converted = convertD4(calcTime);
      else if (state.filmStart === 'D5') converted = convertD5(calcTime);
      else converted = convertD7(calcTime);

      const filmList = ['D4', 'D5', 'D7'];
      filmList.forEach(f => {
        const isStart = (f === state.filmStart);
        const row = document.createElement('div');
        row.className = 'converted-film-row';
        row.innerHTML = `
          <span class="converted-film-name">${f} ${isStart ? '<small style="color:#aaa;font-size:0.65rem">(Basis)</small>' : ''}</span>
          <span class="converted-film-val">${formatTime(converted[f.toLowerCase()])}</span>
        `;
        // Tapping a film starts the timer with that film's time!
        row.style.cursor = 'pointer';
        row.onclick = () => {
          haptic(25);
          startTimer(converted[f.toLowerCase()]);
        };
        filmsGridEl.appendChild(row);
      });

      subtextEl.innerHTML = `Start: <strong>${state.filmStart}</strong> • FFD: ${state.ffdChanged ? `${state.ffdOld}→${state.ffdNew}cm` : 'Vast'} • Factor: ${factor.toFixed(2)}`;
    } else {
      mainTimeEl.style.display = 'block';
      filmsGridEl.style.display = 'none';
      mainTimeEl.textContent = formatTime(calcTime);

      let parts = [];
      if (state.ffdChanged) parts.push(`${state.ffdOld}→${state.ffdNew}cm`);
      if (state.matChanged && factor !== 1.0) parts.push(`Factor ${factor.toFixed(2)}`);
      if (parts.length === 0) parts.push('Geen FFD/Factor correctie');
      parts.push(`(${roundedSec}s)`);

      subtextEl.textContent = parts.join(' • ');
    }

    if (timerBtn) {
      timerBtn.onclick = () => {
        haptic(25);
        startTimer(calcTime);
      };
    }
  }

  // --- DOM UPDATE HELPERS ---
  function updateUI() {
    // Mode tabs
    document.querySelectorAll('.mode-tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.mode === state.mode);
    });

    // Cards visibility by mode
    document.getElementById('card-mode-time').style.display = (state.mode === 'time') ? 'flex' : 'none';
    document.getElementById('card-mode-mamin').style.display = (state.mode === 'mamin') ? 'flex' : 'none';
    document.getElementById('card-mode-ci').style.display = (state.mode === 'ci') ? 'flex' : 'none';

    // Time values
    document.getElementById('val-min').textContent = state.minutes;
    document.getElementById('val-sec').textContent = String(state.seconds).padStart(2, '0');

    // mA*min values
    document.getElementById('val-mamin').textContent = state.mamin.toFixed(1);
    document.getElementById('val-ma').textContent = state.ma.toFixed(1);
    const maminCalcSec = (state.ma > 0) ? (state.mamin / state.ma) * 60 : 0;
    document.getElementById('mamin-preview').textContent = `= ${formatTime(maminCalcSec)}`;

    // Ci values
    document.getElementById('val-ci-oldtime').textContent = formatTime((state.ciOldMin * 60) + state.ciOldSec);
    document.getElementById('val-ci-old').textContent = state.ciOldVal.toFixed(1);
    document.getElementById('val-ci-new').textContent = state.ciNewVal.toFixed(1);

    // FFD
    const ffdSwitch = document.getElementById('toggle-ffd');
    if (ffdSwitch) ffdSwitch.checked = state.ffdChanged;
    document.getElementById('ffd-content').style.display = state.ffdChanged ? 'flex' : 'none';
    document.getElementById('val-ffd-old').textContent = state.ffdOld;
    document.getElementById('val-ffd-new').textContent = state.ffdNew;

    // Film
    const filmSwitch = document.getElementById('toggle-film');
    if (filmSwitch) filmSwitch.checked = state.filmChanged;
    document.getElementById('film-content').style.display = state.filmChanged ? 'flex' : 'none';
    document.querySelectorAll('.film-pill-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.film === state.filmStart);
    });

    // Material
    const matSwitch = document.getElementById('toggle-mat');
    if (matSwitch) matSwitch.checked = state.matChanged;
    document.getElementById('mat-content').style.display = state.matChanged ? 'flex' : 'none';

    document.querySelectorAll('.kv-pill').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.kv === state.selectedKv);
    });

    // Material trigger text
    const matTriggerText = document.getElementById('mat-select-name');
    const matTriggerFactor = document.getElementById('mat-select-factor');
    if (state.selectedMatIdx >= 0 && typeof MATERIALS_DATA !== 'undefined' && MATERIALS_DATA[state.selectedMatIdx]) {
      const mat = MATERIALS_DATA[state.selectedMatIdx];
      const factor = getMaterialFactor(mat, state.selectedKv);
      matTriggerText.textContent = mat.name;
      matTriggerFactor.textContent = `x${factor.toFixed(2)}`;
    } else {
      matTriggerText.textContent = 'Handmatig / Selecteer...';
      matTriggerFactor.textContent = `x${state.manualFactor.toFixed(2)}`;
    }

    calculate();
  }

  // --- TIMER CONTROLS ---
  function startTimer(seconds) {
    const totalSec = Math.max(1, Math.round(seconds));
    state.timerTotalSeconds = totalSec;
    state.timerRemainingSeconds = totalSec;
    state.timerPaused = false;
    state.timerActive = true;

    const overlay = document.getElementById('timer-overlay');
    overlay.classList.add('show');
    overlay.classList.remove('timer-done-flash');

    document.getElementById('timer-label').textContent = 'Belichting Actief';
    document.getElementById('btn-timer-pause').textContent = '⏸';

    updateTimerDisplay();

    if (state.timerInterval) clearInterval(state.timerInterval);
    state.timerInterval = setInterval(tickTimer, 1000);
    haptic([30, 50, 30]);
  }

  function tickTimer() {
    if (state.timerPaused) return;

    state.timerRemainingSeconds--;
    if (state.timerRemainingSeconds <= 0) {
      state.timerRemainingSeconds = 0;
      clearInterval(state.timerInterval);
      state.timerInterval = null;
      onTimerFinished();
    }
    updateTimerDisplay();
  }

  function updateTimerDisplay() {
    const digitsEl = document.getElementById('timer-digits');
    const progressEl = document.getElementById('timer-progress-ring');
    if (!digitsEl || !progressEl) return;

    digitsEl.textContent = formatTimerDigits(state.timerRemainingSeconds);

    const circumference = 565.48; // 2 * PI * 90
    const fraction = state.timerTotalSeconds > 0 ? (state.timerRemainingSeconds / state.timerTotalSeconds) : 0;
    const offset = circumference * (1 - fraction);
    progressEl.style.strokeDashoffset = offset;
  }

  function onTimerFinished() {
    haptic([300, 150, 300, 150, 500]);
    playBeep();
    const overlay = document.getElementById('timer-overlay');
    overlay.classList.add('timer-done-flash');
    document.getElementById('timer-label').textContent = 'Belichting Klaar!';
    document.getElementById('btn-timer-pause').textContent = '↻';
  }

  function toggleTimerPause() {
    haptic(20);
    if (state.timerRemainingSeconds <= 0) {
      // Restart
      startTimer(state.timerTotalSeconds);
      return;
    }
    state.timerPaused = !state.timerPaused;
    document.getElementById('btn-timer-pause').textContent = state.timerPaused ? '▶' : '⏸';
    document.getElementById('timer-label').textContent = state.timerPaused ? 'Gepauzeerd' : 'Belichting Actief';
  }

  function stopTimer() {
    haptic(15);
    if (state.timerInterval) clearInterval(state.timerInterval);
    state.timerInterval = null;
    state.timerActive = false;
    const overlay = document.getElementById('timer-overlay');
    overlay.classList.remove('show', 'timer-done-flash');
  }

  // --- MATERIAL MODAL ---
  function openMaterialModal() {
    haptic(15);
    const modal = document.getElementById('mat-modal');
    modal.classList.add('show');
    renderMaterialList('');
    const input = document.getElementById('mat-search-input');
    input.value = '';
  }

  function closeMaterialModal() {
    haptic(15);
    const modal = document.getElementById('mat-modal');
    modal.classList.remove('show');
  }

  function renderMaterialList(query) {
    const listEl = document.getElementById('mat-list');
    if (!listEl || typeof MATERIALS_DATA === 'undefined') return;

    listEl.innerHTML = '';
    const q = (query || '').toLowerCase().trim();

    const filtered = MATERIALS_DATA.map((mat, idx) => ({ mat, idx })).filter(({ mat }) => {
      if (!q) return true;
      const text = `${mat.name} ${mat.werkstoff || ''} ${mat.group || ''} ${mat.notes || ''}`.toLowerCase();
      return text.includes(q);
    });

    if (filtered.length === 0) {
      listEl.innerHTML = '<div style="color:#888;text-align:center;padding:15px">Geen materiaal gevonden.</div>';
      return;
    }

    filtered.forEach(({ mat, idx }) => {
      const factor = getMaterialFactor(mat, state.selectedKv);
      const isSel = (idx === state.selectedMatIdx);
      const item = document.createElement('div');
      item.className = `mat-item ${isSel ? 'active' : ''}`;
      item.innerHTML = `
        <div class="mat-item-info">
          <div class="mat-item-name">${mat.name}</div>
          <div class="mat-item-detail">${mat.werkstoff ? `${mat.werkstoff} • ` : ''}${mat.group}</div>
        </div>
        <div class="mat-item-factor">x${factor.toFixed(2)}</div>
      `;
      item.onclick = () => {
        haptic(20);
        state.selectedMatIdx = idx;
        state.manualFactor = factor;
        closeMaterialModal();
        updateUI();
      };
      listEl.appendChild(item);
    });
  }

  // --- SOURCES FOR CI TAB ---
  function initSourcesDropdown() {
    const select = document.getElementById('ci-source-select');
    if (!select || typeof SOURCES_DATA === 'undefined') return;

    select.innerHTML = '<option value="-1">Handmatige Ci invoer...</option>';
    SOURCES_DATA.forEach((src, idx) => {
      const curCi = calculateCurrentCi(src.initialCi, src.type, src.date);
      const opt = document.createElement('option');
      opt.value = idx;
      opt.textContent = `${src.type} ${src.name} (${curCi.toFixed(1)} Ci)`;
      select.appendChild(opt);
    });

    select.onchange = () => {
      const idx = parseInt(select.value);
      state.ciSelectedSourceIdx = idx;
      if (idx >= 0 && SOURCES_DATA[idx]) {
        const src = SOURCES_DATA[idx];
        const curCi = calculateCurrentCi(src.initialCi, src.type, src.date);
        state.ciNewVal = parseFloat(curCi.toFixed(2));
      }
      haptic(15);
      updateUI();
    };
  }

  // --- ROTARY CROWN SCROLL ---
  function setupRotarySupport() {
    const scrollContainer = document.getElementById('watch-screen');
    if (!scrollContainer) return;

    window.addEventListener('wheel', (e) => {
      // Pixel Watch 3 rotary crown sends standard wheel events
      scrollContainer.scrollBy({
        top: e.deltaY * 0.8,
        behavior: 'auto'
      });
      // Subtle tick on crown movement
      haptic(5);
    }, { passive: true });
  }

  // --- PRESETS (LOCAL STORAGE) ---
  function savePreset() {
    haptic(30);
    const presets = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    const name = prompt('Preset naam:', `FFD ${state.ffdNew}cm (${state.filmStart})`);
    if (!name) return;

    presets.push({
      id: Date.now(),
      name: name,
      mode: state.mode,
      minutes: state.minutes,
      seconds: state.seconds,
      mamin: state.mamin,
      ma: state.ma,
      ffdChanged: state.ffdChanged,
      ffdOld: state.ffdOld,
      ffdNew: state.ffdNew,
      filmChanged: state.filmChanged,
      filmStart: state.filmStart,
      matChanged: state.matChanged,
      selectedKv: state.selectedKv,
      selectedMatIdx: state.selectedMatIdx,
      manualFactor: state.manualFactor
    });

    localStorage.setItem(STORAGE_KEY, JSON.stringify(presets));
    alert('Preset opgeslagen!');
  }

  function openPresetsModal() {
    haptic(20);
    const modal = document.getElementById('presets-modal');
    modal.classList.add('show');
    renderPresetsList();
  }

  function closePresetsModal() {
    haptic(15);
    const modal = document.getElementById('presets-modal');
    modal.classList.remove('show');
  }

  function renderPresetsList() {
    const listEl = document.getElementById('presets-list');
    if (!listEl) return;
    const presets = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    listEl.innerHTML = '';

    if (presets.length === 0) {
      listEl.innerHTML = '<div style="color:#888;text-align:center;padding:15px">Geen opgeslagen presets.<br>Tik op "Opslaan" om een situatie te bewaren.</div>';
      return;
    }

    presets.slice().reverse().forEach(p => {
      const item = document.createElement('div');
      item.className = 'mat-item';
      item.innerHTML = `
        <div class="mat-item-info">
          <div class="mat-item-name">${p.name}</div>
          <div class="mat-item-detail">FFD ${p.ffdOld}→${p.ffdNew} • ${p.filmStart}</div>
        </div>
        <button style="background:none;border:none;color:#ff5555;font-size:1.1rem;padding:6px">&times;</button>
      `;
      // Load on click
      item.onclick = (e) => {
        if (e.target.tagName === 'BUTTON') {
          // Delete
          const updated = presets.filter(x => x.id !== p.id);
          localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
          haptic(25);
          renderPresetsList();
          return;
        }
        // Apply preset
        haptic(30);
        Object.assign(state, p);
        closePresetsModal();
        updateUI();
      };
      listEl.appendChild(item);
    });
  }

  // --- EVENT ATTACHMENTS ---
  function initListeners() {
    // Mode tabs
    document.querySelectorAll('.mode-tab-btn').forEach(btn => {
      btn.onclick = () => {
        haptic(15);
        state.mode = btn.dataset.mode;
        updateUI();
      };
    });

    // Time steppers
    document.getElementById('btn-min-minus').onclick = () => {
      haptic(15);
      state.minutes = Math.max(0, state.minutes - 1);
      updateUI();
    };
    document.getElementById('btn-min-plus').onclick = () => {
      haptic(15);
      state.minutes++;
      updateUI();
    };
    document.getElementById('btn-sec-minus').onclick = () => {
      haptic(15);
      state.seconds = Math.max(0, state.seconds - 5);
      updateUI();
    };
    document.getElementById('btn-sec-plus').onclick = () => {
      haptic(15);
      state.seconds = (state.seconds + 5) % 60;
      updateUI();
    };

    // Time Presets
    document.querySelectorAll('.chip-time').forEach(chip => {
      chip.onclick = () => {
        haptic(15);
        const m = parseInt(chip.dataset.m) || 0;
        const s = parseInt(chip.dataset.s) || 0;
        state.minutes = m;
        state.seconds = s;
        updateUI();
      };
    });

    // mA*min steppers
    document.getElementById('btn-mamin-minus').onclick = () => {
      haptic(15);
      state.mamin = Math.max(0.5, parseFloat((state.mamin - 0.5).toFixed(1)));
      updateUI();
    };
    document.getElementById('btn-mamin-plus').onclick = () => {
      haptic(15);
      state.mamin = parseFloat((state.mamin + 0.5).toFixed(1));
      updateUI();
    };
    document.getElementById('btn-ma-minus').onclick = () => {
      haptic(15);
      state.ma = Math.max(0.5, parseFloat((state.ma - 0.5).toFixed(1)));
      updateUI();
    };
    document.getElementById('btn-ma-plus').onclick = () => {
      haptic(15);
      state.ma = parseFloat((state.ma + 0.5).toFixed(1));
      updateUI();
    };

    // Ci steppers
    document.getElementById('btn-ci-oldtime-minus').onclick = () => {
      haptic(15);
      let total = (state.ciOldMin * 60) + state.ciOldSec - 10;
      if (total < 0) total = 0;
      state.ciOldMin = Math.floor(total / 60);
      state.ciOldSec = total % 60;
      updateUI();
    };
    document.getElementById('btn-ci-oldtime-plus').onclick = () => {
      haptic(15);
      let total = (state.ciOldMin * 60) + state.ciOldSec + 10;
      state.ciOldMin = Math.floor(total / 60);
      state.ciOldSec = total % 60;
      updateUI();
    };
    document.getElementById('btn-ci-old-minus').onclick = () => {
      haptic(15);
      state.ciOldVal = Math.max(1, parseFloat((state.ciOldVal - 5).toFixed(1)));
      updateUI();
    };
    document.getElementById('btn-ci-old-plus').onclick = () => {
      haptic(15);
      state.ciOldVal = parseFloat((state.ciOldVal + 5).toFixed(1));
      updateUI();
    };
    document.getElementById('btn-ci-new-minus').onclick = () => {
      haptic(15);
      state.ciNewVal = Math.max(1, parseFloat((state.ciNewVal - 5).toFixed(1)));
      updateUI();
    };
    document.getElementById('btn-ci-new-plus').onclick = () => {
      haptic(15);
      state.ciNewVal = parseFloat((state.ciNewVal + 5).toFixed(1));
      updateUI();
    };

    // FFD toggle & steppers
    document.getElementById('toggle-ffd').onchange = (e) => {
      haptic(15);
      state.ffdChanged = e.target.checked;
      updateUI();
    };
    document.getElementById('btn-ffd-old-minus').onclick = () => {
      haptic(15);
      state.ffdOld = Math.max(10, state.ffdOld - 5);
      updateUI();
    };
    document.getElementById('btn-ffd-old-plus').onclick = () => {
      haptic(15);
      state.ffdOld += 5;
      updateUI();
    };
    document.getElementById('btn-ffd-new-minus').onclick = () => {
      haptic(15);
      state.ffdNew = Math.max(10, state.ffdNew - 5);
      updateUI();
    };
    document.getElementById('btn-ffd-new-plus').onclick = () => {
      haptic(15);
      state.ffdNew += 5;
      updateUI();
    };
    document.querySelectorAll('.chip-ffd').forEach(chip => {
      chip.onclick = () => {
        haptic(15);
        state.ffdNew = parseInt(chip.dataset.val);
        updateUI();
      };
    });

    // Film toggle & pills
    document.getElementById('toggle-film').onchange = (e) => {
      haptic(15);
      state.filmChanged = e.target.checked;
      updateUI();
    };
    document.querySelectorAll('.film-pill-btn').forEach(btn => {
      btn.onclick = () => {
        haptic(15);
        state.filmStart = btn.dataset.film;
        updateUI();
      };
    });

    // Material toggle & kV pills
    document.getElementById('toggle-mat').onchange = (e) => {
      haptic(15);
      state.matChanged = e.target.checked;
      updateUI();
    };
    document.querySelectorAll('.kv-pill').forEach(btn => {
      btn.onclick = () => {
        haptic(15);
        state.selectedKv = btn.dataset.kv;
        updateUI();
      };
    });

    // Material search & modal
    document.getElementById('btn-open-mat-modal').onclick = openMaterialModal;
    document.getElementById('btn-close-mat-modal').onclick = closeMaterialModal;
    document.getElementById('mat-search-input').oninput = (e) => {
      renderMaterialList(e.target.value);
    };

    // Timer modal buttons
    document.getElementById('btn-timer-close').onclick = stopTimer;
    document.getElementById('btn-timer-pause').onclick = toggleTimerPause;
    document.getElementById('btn-timer-reset').onclick = () => {
      haptic(20);
      startTimer(state.timerTotalSeconds);
    };

    // Presets
    document.getElementById('btn-save-preset').onclick = savePreset;
    document.getElementById('btn-open-presets').onclick = openPresetsModal;
    document.getElementById('btn-close-presets').onclick = closePresetsModal;

    // Wheel Picker controls
    document.getElementById('btn-wheel-cancel').onclick = closeWheel;
    document.getElementById('btn-wheel-confirm').onclick = confirmWheel;

    // Wheel Picker listeners on value boxes
    document.getElementById('wrap-val-min').onclick = () => {
      const vals = [];
      for (let i = 0; i <= 120; i++) vals.push(i + ' m');
      openWheel('Kies Minuten', vals, state.minutes, (idx) => {
        state.minutes = idx;
      });
    };

    document.getElementById('wrap-val-sec').onclick = () => {
      const vals = [];
      for (let i = 0; i < 60; i++) vals.push(String(i).padStart(2, '0') + ' s');
      openWheel('Kies Seconden', vals, state.seconds, (idx) => {
        state.seconds = idx;
      });
    };

    document.getElementById('wrap-val-ffd-new').onclick = () => {
      const vals = [];
      let sel = 0;
      for (let f = 20; f <= 250; f += 5) {
        vals.push(f + ' cm');
        if (f === state.ffdNew || Math.abs(f - state.ffdNew) < 3) sel = vals.length - 1;
      }
      openWheel('Nieuwe FFD', vals, sel, (idx, val) => {
        state.ffdNew = parseInt(val);
      });
    };
  }

  // --- WEB WHEEL PICKER LOGIC ---
  let wheelCallback = null;
  let activeWheelValues = [];
  let selectedWheelIdx = 0;

  function openWheel(title, values, selectedIdx, callback) {
    haptic(20);
    wheelCallback = callback;
    activeWheelValues = values;
    selectedWheelIdx = Math.max(0, Math.min(values.length - 1, selectedIdx));

    document.getElementById('wheel-picker-title').textContent = title;
    const container = document.getElementById('wheel-slot-container');
    container.innerHTML = '';

    const padTop = document.createElement('div');
    padTop.style.height = '48px';
    container.appendChild(padTop);

    values.forEach((v, i) => {
      const item = document.createElement('div');
      item.className = `wheel-slot-item ${i === selectedWheelIdx ? 'selected' : ''}`;
      item.textContent = v;
      item.onclick = () => {
        haptic(10);
        selectedWheelIdx = i;
        updateWheelSelection();
        item.scrollIntoView({ behavior: 'smooth', block: 'center' });
      };
      container.appendChild(item);
    });

    const padBottom = document.createElement('div');
    padBottom.style.height = '48px';
    container.appendChild(padBottom);

    document.getElementById('wheel-picker-overlay').classList.add('show');

    setTimeout(() => {
      const items = container.querySelectorAll('.wheel-slot-item');
      if (items[selectedWheelIdx]) {
        items[selectedWheelIdx].scrollIntoView({ block: 'center' });
      }
    }, 50);
  }

  function updateWheelSelection() {
    const items = document.querySelectorAll('.wheel-slot-item');
    items.forEach((it, idx) => {
      it.classList.toggle('selected', idx === selectedWheelIdx);
    });
  }

  function closeWheel() {
    haptic(15);
    document.getElementById('wheel-picker-overlay').classList.remove('show');
    wheelCallback = null;
  }

  function confirmWheel() {
    haptic(25);
    if (wheelCallback && activeWheelValues[selectedWheelIdx] !== undefined) {
      wheelCallback(selectedWheelIdx, activeWheelValues[selectedWheelIdx]);
    }
    closeWheel();
    updateUI();
  }

  // --- INITIALIZATION ---
  document.addEventListener('DOMContentLoaded', () => {
    updateClock();
    setInterval(updateClock, 10000);
    initSourcesDropdown();
    initListeners();
    setupRotarySupport();
    updateUI();

    // Register Service Worker for offline capability
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('service-worker.js').catch(() => {});
    }
  });

})();
