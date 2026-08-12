// All calls go to the same Spring Boot app that serves this page,
// so relative paths work whether running on localhost or after deployment.
const API_BASE = '/api/urls';

const shortenForm = document.getElementById('shorten-form');
const urlInput = document.getElementById('url-input');
const expiryInput = document.getElementById('expiry-input');
const submitBtn = document.getElementById('submit-btn');
const errorBox = document.getElementById('error-box');
const resultBox = document.getElementById('result-box');
const resultLink = document.getElementById('result-link');
const resultOriginal = document.getElementById('result-original');
const copyBtn = document.getElementById('copy-btn');

shortenForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  hide(errorBox);
  hide(resultBox);
  submitBtn.disabled = true;
  submitBtn.textContent = 'Shortening…';

  try {
    const body = { url: urlInput.value.trim() };
    if (expiryInput.value) body.expiresInMinutes = Number(expiryInput.value);

    const res = await fetch(`${API_BASE}/shorten`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    const data = await res.json();

    if (!res.ok) {
      throw new Error(data.message || 'Something went wrong');
    }

    resultLink.href = data.shortUrl;
    resultLink.textContent = data.shortUrl;
    resultOriginal.textContent = `→ redirects to: ${data.originalUrl}`;
    show(resultBox);
  } catch (err) {
    errorBox.textContent = err.message;
    show(errorBox);
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Shorten →';
  }
});

copyBtn.addEventListener('click', async () => {
  await navigator.clipboard.writeText(resultLink.href);
  const original = copyBtn.textContent;
  copyBtn.textContent = 'Copied!';
  setTimeout(() => (copyBtn.textContent = original), 1200);
});

const statsForm = document.getElementById('stats-form');
const statsInput = document.getElementById('stats-input');
const statsResult = document.getElementById('stats-result');

statsForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  hide(statsResult);

  try {
    // Accept either a bare code ("4c92") or a full pasted short URL.
    const raw = statsInput.value.trim();
    const code = raw.includes('/r/') ? raw.split('/r/').pop() : raw;

    const res = await fetch(`${API_BASE}/${encodeURIComponent(code)}/stats`);
    const data = await res.json();

    if (!res.ok) throw new Error(data.message || 'Short code not found');

    statsResult.innerHTML = `
      <div><b>${data.clickCount}</b> click${data.clickCount === 1 ? '' : 's'}</div>
      <div>created: ${new Date(data.createdAt).toLocaleString()}</div>
      <div>${data.expiresAt ? 'expires: ' + new Date(data.expiresAt).toLocaleString() : 'no expiry'}</div>
    `;
    show(statsResult);
  } catch (err) {
    statsResult.innerHTML = `<span style="color:#f87171">${err.message}</span>`;
    show(statsResult);
  }
});

function show(el) { el.classList.remove('hidden'); }
function hide(el) { el.classList.add('hidden'); }
