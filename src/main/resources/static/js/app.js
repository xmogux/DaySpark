const modal = document.getElementById("completeModal");
const closeModal = document.getElementById("closeModal");
const note = document.getElementById("note");
const charCount = document.getElementById("charCount");
const image = document.getElementById("image");
const fileName = document.getElementById("fileName");
const dareId = document.getElementById("dareId");
const modalTitle = document.getElementById("modalTitle");
function setModal(open) {
  if (!modal) return;
  modal.classList.toggle("open", open);
  modal.setAttribute("aria-hidden", open ? "false" : "true");
  document.body.style.overflow = open ? "hidden" : "";
}
document.querySelectorAll(".complete-trigger").forEach((btn) =>
  btn.addEventListener("click", () => {
    if (dareId) dareId.value = btn.dataset.id;
    if (modalTitle) modalTitle.textContent = btn.dataset.title;
    setModal(true);
  }),
);
if (closeModal) closeModal.addEventListener("click", () => setModal(false));
if (modal)
  modal.addEventListener("click", (e) => {
    if (e.target === modal) setModal(false);
  });
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") setModal(false);
});
if (note && charCount) note.addEventListener("input", () => (charCount.textContent = note.value.length));
if (image && fileName)
  image.addEventListener("change", () => (fileName.textContent = image.files?.[0]?.name || "선택된 파일 없음"));
const repeatRadios = [...document.querySelectorAll('input[name="repeatType"]')];
const customDays = document.getElementById("customDays");
function updateCustom() {
  if (!customDays) return;
  const v = document.querySelector('input[name="repeatType"]:checked')?.value;
  customDays.classList.toggle("show", v === "CUSTOM");
}
repeatRadios.forEach((r) => r.addEventListener("change", updateCustom));
updateCustom();

// v5.5: study review-plan controls
const categorySelect = document.querySelector('select[name="category"]');
const reviewSettings = document.getElementById("reviewSettings");
const reviewEnabled = document.getElementById("reviewEnabled");
const reviewFields = document.getElementById("reviewFields");
const startDateInput = document.querySelector('input[name="startDate"]');
const reviewBaseDate = document.getElementById("reviewBaseDate");
const reviewIntervalsInput = document.getElementById("reviewIntervals");
const reviewPreviewDates = document.getElementById("reviewPreviewDates");
function updateReviewPreview() {
  if (!reviewPreviewDates) return;
  reviewPreviewDates.innerHTML = "";
  if (!reviewEnabled?.checked || !reviewBaseDate?.value || !reviewIntervalsInput?.value) return;
  const offsets = [
    ...new Set(
      reviewIntervalsInput.value
        .split(/[,\s]+/)
        .map((v) => Number(v))
        .filter((v) => Number.isInteger(v) && v > 0 && v <= 3650),
    ),
  ]
    .sort((a, b) => a - b)
    .slice(0, 12);
  const base = new Date(reviewBaseDate.value + "T00:00:00");
  offsets.forEach((offset, index) => {
    const due = new Date(base);
    due.setDate(due.getDate() + offset);
    const chip = document.createElement("span");
    chip.textContent = `${index + 1}차 · ${due.getMonth() + 1}/${due.getDate()} (+${offset}일)`;
    reviewPreviewDates.appendChild(chip);
  });
}
function updateReviewSettings() {
  if (!reviewSettings) return;
  const isStudy = categorySelect?.value === "STUDY";
  reviewSettings.classList.toggle("show", isStudy);
  if (!isStudy) {
    if (reviewEnabled) reviewEnabled.checked = false;
  }
  const enabled = Boolean(isStudy && reviewEnabled?.checked);
  reviewFields?.classList.toggle("show", enabled);
  reviewFields?.querySelectorAll("input").forEach((input) => (input.disabled = !enabled));
  updateReviewPreview();
}
categorySelect?.addEventListener("change", updateReviewSettings);
reviewEnabled?.addEventListener("change", () => {
  if (reviewEnabled.checked && reviewBaseDate && !reviewBaseDate.value && startDateInput)
    reviewBaseDate.value = startDateInput.value;
  updateReviewSettings();
});
startDateInput?.addEventListener("change", () => {
  if (reviewBaseDate && reviewEnabled?.checked && !reviewBaseDate.value) reviewBaseDate.value = startDateInput.value;
  updateReviewPreview();
});
reviewBaseDate?.addEventListener("change", updateReviewPreview);
reviewIntervalsInput?.addEventListener("input", updateReviewPreview);
updateReviewSettings();
