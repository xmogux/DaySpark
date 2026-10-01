(() => {
  "use strict";
  const $ = (id) => document.getElementById(id);
  const circumference = 2 * Math.PI * 120,
    secondsCircumference = 2 * Math.PI * 98,
    PREVIEW_MS = 15000;
  let session = null,
    phase = "idle",
    round = 1,
    plan = null,
    deadline = 0,
    breakRemaining = 0,
    finishing = false,
    finishRetryAt = 0;
  const FINISH_RETRY_MS = 5000; // after a failed finish, wait before asking the server again (no 10 req/s loop)
  let previewDeadline = 0,
    previewRemaining = PREVIEW_MS;
  const arc = $("timerArc"),
    secondsArc = $("timerSecondsArc"),
    readout = $("timerReadout");
  const status = $("timerStatus"),
    start = $("startTimer"),
    pause = $("pauseTimer"),
    preview = $("previewTimer");
  const savedKey = "dayspark-focus-settings-v3";
  function getNumber(id, min, max) {
    const n = Number($(id).value);
    if (!Number.isInteger(n) || n < min || n > max) throw Error(`${id} 값은 ${min}~${max} 사이 정수여야 합니다.`);
    return n;
  }
  function setStatus(msg, isError = false) {
    status.textContent = msg;
    status.classList.toggle("is-error", isError);
  }
  function readSettings() {
    return {
      dareId: $("focusDare").value,
      minutes: getNumber("focusMinutes", 1, 120),
      rest: getNumber("breakMinutes", 0, 30),
      repeats: getNumber("repeatCount", 1, 10),
    };
  }
  function showStats() {
    fetch("/api/focus/summary")
      .then((r) => r.json())
      .then((x) => {
        $("todayMinutes").textContent = `${x.todayMinutes}분`;
        $("weekMinutes").textContent = `${x.weekMinutes}분`;
        $("weekCount").textContent = `${x.weekCount}회`;
      })
      .catch(() => {});
  }
  async function request(path, params) {
    const opts =
      params === undefined
        ? {}
        : {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams(params),
          };
    const res = await fetch("/api/focus" + path, opts);
    const data = await res.json();
    if (!res.ok) throw Error(data.error || "서버가 요청을 처리하지 못했습니다.");
    return data;
  }
  function savePlan() {
    localStorage.setItem(savedKey, JSON.stringify({ plan, round }));
  }
  function clearPlan() {
    localStorage.removeItem(savedKey);
  }
  function fmt(ms) {
    const sec = Math.max(0, Math.ceil(ms / 1000));
    return `${String(Math.floor(sec / 60)).padStart(2, "0")}:${String(sec % 60).padStart(2, "0")}`;
  }
  function elapsedFmt(ms) {
    const sec = Math.max(0, Math.floor(ms / 1000));
    return `${String(Math.floor(sec / 60)).padStart(2, "0")}:${String(sec % 60).padStart(2, "0")}`;
  }
  function draw(ms, total, label) {
    const remaining = Math.min(Math.max(ms, 0), Math.max(total, 0));
    const elapsed = Math.max(0, total - remaining),
      percent = total ? Math.min(1, elapsed / total) : 0;
    // At ordinary 25-minute lengths, the OUTER ring advances slowly. The INNER ring
    // sweeps once per minute so each second is visibly reflected on screen.
    const secondsCycle = Math.min(Math.max(total, 1000), 60000);
    const secondsElapsed = total <= 60000 ? elapsed : elapsed % 60000;
    const secondsFraction = Math.min(1, secondsElapsed / secondsCycle);
    readout.textContent = fmt(remaining);
    $("timerSubtext").textContent = label;
    arc.style.strokeDasharray = String(circumference);
    arc.style.strokeDashoffset = String(circumference * (1 - percent));
    secondsArc.style.strokeDasharray = String(secondsCircumference);
    secondsArc.style.strokeDashoffset = String(secondsCircumference * (1 - secondsFraction));
    $("totalMeterFill").style.width = `${percent * 100}%`;
    $("secondsMeterFill").style.width = `${secondsFraction * 100}%`;
    $("timerPercent").textContent = `${Math.floor(percent * 100)}%`;
    $("elapsedLabel").textContent = `${elapsedFmt(elapsed)} / ${fmt(total)}`;
    $("secondsMeterLabel").textContent =
      total <= 60000 ? `${Math.ceil(secondsCycle / 1000)}초 흐름` : "매초 움직이는 60초 게이지";
    $("secondsLabel").textContent = `${Math.floor(secondsElapsed / 1000)} / ${Math.ceil(secondsCycle / 1000)}초`;
    const resting = phase === "break" || phase === "breakpaused";
    $("timerVisual").classList.toggle("break-mode", resting);
    $("timerMode").textContent =
      phase === "preview" || phase === "previewpaused"
        ? "15 SECOND PREVIEW"
        : resting
          ? "REST & RESET"
          : phase === "idle"
            ? "READY TO FOCUS"
            : "FOCUS IN PROGRESS";
  }
  function configDisabled(disabled) {
    for (const id of ["focusDare", "focusMinutes", "breakMinutes", "repeatCount"]) $(id).disabled = disabled;
  }
  function drawButtons() {
    const resting = phase === "break" || phase === "breakpaused",
      focusing = phase === "focus",
      previewing = phase === "preview" || phase === "previewpaused";
    start.textContent =
      phase === "idle"
        ? "집중 시작 ▶"
        : focusing
          ? "진행 중..."
          : resting
            ? "휴식 중..."
            : previewing
              ? "시각 테스트 중..."
              : "진행 중...";
    start.disabled = phase !== "idle";
    pause.disabled = !(focusing || phase === "paused" || resting || previewing);
    pause.textContent = ["paused", "breakpaused", "previewpaused"].includes(phase) ? "재개" : "일시정지";
    $("resetTimer").disabled = phase === "idle";
    preview.disabled = phase !== "idle";
    $("focusPhase").textContent = previewing
      ? "VISUAL TEST"
      : resting
        ? "BREAK TIME"
        : phase === "idle"
          ? "GET READY"
          : "FOCUS MODE";
    $("roundLabel").textContent = previewing
      ? "15초 · 저장 안 함"
      : `${round} / ${plan?.repeats || $("repeatCount").value}회차`;
    configDisabled(phase !== "idle");
  }
  function refresh() {
    if (phase === "preview" && Date.now() >= previewDeadline) {
      phase = "idle";
      previewRemaining = PREVIEW_MS;
      setStatus("15초 시각 테스트 완료! 이 시간은 통계에 저장되지 않았어요.");
    }
    drawButtons();
    if (phase === "idle") {
      const total = Number($("focusMinutes").value || 25) * 60000;
      draw(total, total, "시작할 준비가 됐어요");
      return;
    }
    if (phase === "preview") {
      draw(Math.max(0, previewDeadline - Date.now()), PREVIEW_MS, "15초 동안 게이지가 채워져요");
      return;
    }
    if (phase === "previewpaused") {
      draw(previewRemaining, PREVIEW_MS, "시각 테스트 일시정지");
      return;
    }
    if (phase === "focus" && session) {
      const remain = Math.max(0, session.remainingMillis - (Date.now() - session.syncedAt));
      draw(remain, session.durationMinutes * 60000, session.dareTitle + "에 집중 중");
      if (remain === 0 && !finishing && Date.now() >= finishRetryAt) finishRound();
    }
    if (phase === "paused" && session) draw(session.remainingMillis, session.durationMinutes * 60000, "일시정지 중");
    if (phase === "break") {
      const remain = Math.max(0, deadline - Date.now());
      draw(remain, (plan?.rest || 0) * 60000, "다음 집중 전 쉬는 시간");
      if (remain === 0 && !finishing) startNext();
    }
    if (phase === "breakpaused") draw(breakRemaining, (plan?.rest || 0) * 60000, "휴식 일시정지 중");
  }
  async function beginFocus(isNext = false) {
    if (finishing || (!isNext && phase !== "idle")) return;
    try {
      if (!isNext) {
        plan = readSettings();
        if (!plan.dareId) throw Error("연결할 도전을 선택해주세요.");
        round = 1;
      }
      phase = "loading";
      refresh();
      const data = await request("/start", { dareId: plan.dareId, minutes: plan.minutes });
      session = { ...data, syncedAt: Date.now() };
      phase = "focus";
      savePlan();
      setStatus(`${round}회차 집중 시작! 바깥은 전체, 안쪽은 매초 움직여요.`);
    } catch (e) {
      setStatus(e.message, true);
      phase = "idle";
      if (isNext) {
        clearPlan();
        plan = null;
        round = 1;
      }
    }
    refresh();
  }
  async function finishRound() {
    if (finishing || !session) return;
    finishing = true;
    try {
      const s = await request(`/${session.id}/finish`, {});
      if (s.status !== "COMPLETED") throw Error("세션을 완료하지 못했어요.");
      showStats();
      session = null;
      if (round >= plan.repeats) {
        phase = "idle";
        setStatus(`축하해요! ${plan.repeats}회 반복 완료. 누적 기록은 통계·달력에서 확인하세요.`);
        clearPlan();
        round = 1;
        plan = null;
      } else {
        phase = "break";
        deadline = Date.now() + plan.rest * 60000;
        setStatus(`${round}회차 완료! ${plan.rest}분 휴식 후 다음 집중이 시작돼요.`);
      }
    } catch (e) {
      setStatus(e.message, true);
      finishRetryAt = Date.now() + FINISH_RETRY_MS;
      await resyncAfterFailedFinish();
    } finally {
      finishing = false;
      refresh();
    }
  }
  // The server is the source of truth. After a failed finish (clock skew, session cancelled in
  // another tab, server restart) adopt the server's state instead of retrying blindly.
  async function resyncAfterFailedFinish() {
    try {
      const current = await request("/current");
      if (current.active === false) {
        session = null;
        phase = "idle";
        plan = null;
        round = 1;
        clearPlan();
        setStatus("진행 중인 집중 세션이 없어 타이머를 초기화했어요.");
        return;
      }
      session = { ...current, syncedAt: Date.now() };
      phase = current.status === "PAUSED" ? "paused" : "focus";
    } catch (_) {
      /* offline: keep the current view and retry after FINISH_RETRY_MS */
    }
  }
  async function startNext() {
    if (finishing || phase !== "break") return;
    round++;
    phase = "loading";
    savePlan();
    await beginFocus(true);
  }
  function beginPreview() {
    if (phase !== "idle") return;
    phase = "preview";
    previewDeadline = Date.now() + PREVIEW_MS;
    previewRemaining = PREVIEW_MS;
    setStatus("15초 시각 테스트 중 · 통계와 집중 회차에는 저장되지 않아요.");
    refresh();
  }
  async function togglePause() {
    if (finishing) return;
    try {
      if (phase === "focus") {
        session = { ...(await request(`/${session.id}/pause`, {})), syncedAt: Date.now() };
        phase = "paused";
        setStatus("일시정지했어요.");
      } else if (phase === "paused") {
        session = { ...(await request(`/${session.id}/resume`, {})), syncedAt: Date.now() };
        phase = "focus";
        setStatus("집중을 이어갑니다.");
      } else if (phase === "break") {
        breakRemaining = Math.max(0, deadline - Date.now());
        phase = "breakpaused";
        setStatus("휴식을 일시정지했어요.");
      } else if (phase === "breakpaused") {
        deadline = Date.now() + breakRemaining;
        phase = "break";
        setStatus("휴식을 다시 시작했어요.");
      } else if (phase === "preview") {
        previewRemaining = Math.max(0, previewDeadline - Date.now());
        phase = "previewpaused";
        setStatus("시각 테스트 일시정지");
      } else if (phase === "previewpaused") {
        previewDeadline = Date.now() + previewRemaining;
        phase = "preview";
        setStatus("시각 테스트 재개");
      }
    } catch (e) {
      setStatus(e.message, true);
    }
    refresh();
  }
  async function cancel() {
    if (phase === "idle") return;
    if (phase === "preview" || phase === "previewpaused") {
      phase = "idle";
      previewRemaining = PREVIEW_MS;
      setStatus("시각 테스트 종료 · 기록은 남지 않아요.");
      refresh();
      return;
    }
    if (!confirm("진행 중인 회차를 중단할까요? 미완료 집중 시간은 누적되지 않습니다.")) return;
    try {
      if (session) await request(`/${session.id}/cancel`, {});
      session = null;
      phase = "idle";
      plan = null;
      round = 1;
      clearPlan();
      setStatus("중단했어요. 완료한 회차 기록은 그대로 보관돼요.");
    } catch (e) {
      setStatus(e.message, true);
    }
    refresh();
  }
  async function restore() {
    try {
      const prev = JSON.parse(localStorage.getItem(savedKey) || "null");
      if (prev && prev.plan) {
        plan = prev.plan;
        round = Math.max(1, Math.min(prev.round || 1, prev.plan.repeats || 1));
        $("focusDare").value = plan.dareId;
        $("focusMinutes").value = plan.minutes;
        $("breakMinutes").value = plan.rest;
        $("repeatCount").value = plan.repeats;
      }
      const active = await request("/current");
      if (active.active !== false) {
        session = { ...active, syncedAt: Date.now() };
        if (!plan || plan.dareId !== String(active.dareId) || plan.minutes !== active.durationMinutes) {
          plan = { dareId: String(active.dareId), minutes: active.durationMinutes, rest: 5, repeats: 1 };
          round = 1;
        }
        $("focusDare").value = active.dareId;
        $("focusMinutes").value = active.durationMinutes;
        $("breakMinutes").value = plan.rest;
        $("repeatCount").value = plan.repeats;
        phase = active.status === "PAUSED" ? "paused" : "focus";
        setStatus("서버에 저장된 세션을 복원했어요.");
      } else {
        clearPlan();
        plan = null;
        round = 1;
        phase = "idle";
      }
    } catch (e) {
      setStatus("서버 연결 실패: " + e.message, true);
    }
    refresh();
    showStats();
  }
  start.addEventListener("click", () => beginFocus());
  pause.addEventListener("click", togglePause);
  $("resetTimer").addEventListener("click", cancel);
  preview.addEventListener("click", beginPreview);
  $("focusMinutes").addEventListener("input", () => {
    if (phase === "idle") refresh();
  });
  setInterval(refresh, 100);
  restore();
})();
