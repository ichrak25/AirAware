// Global State
const state = {
  isAuthenticated: false,
  userEmail: "",
  currentPage: "dashboard",
  isDarkMode: false,
  metrics: {
    aqi: 42,
    co2: 412,
    pm25: 8.5,
    pm10: 15.2,
    temperature: 22.3,
    humidity: 55,
    pressure: 1013,
  },
  historicalData: [],
}

// Initialize
document.addEventListener("DOMContentLoaded", () => {
  setupPWA()
  setupEventListeners()
  loadLocalStorage()
  checkAuth()
  startDataSimulation()
})

// PWA Setup
function setupPWA() {
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker
      .register("sw.js")
      .catch((err) => console.log("[v0] Service worker registration failed:", err))
  }

  let deferredPrompt
  window.addEventListener("beforeinstallprompt", (e) => {
    e.preventDefault()
    deferredPrompt = e
    document.getElementById("pwa-prompt").classList.remove("hidden")
  })

  window.addEventListener("appinstalled", () => {
    console.log("[v0] PWA installed")
    document.getElementById("pwa-prompt").classList.add("hidden")
  })

  window.installPWA = () => {
    if (deferredPrompt) {
      deferredPrompt.prompt()
      deferredPrompt = null
      dismissPWA()
    }
  }
}

// Event Listeners
function setupEventListeners() {
  const authForm = document.getElementById("auth-form")
  if (authForm) {
    authForm.addEventListener("submit", handleLogin)
  }

  // Dark mode
  const isDarkMode = localStorage.getItem("darkMode") === "true"
  if (isDarkMode) {
    document.body.classList.add("dark-mode")
    state.isDarkMode = true
  }
}

// Authentication
function handleLogin(e) {
  e.preventDefault()
  const email = document.getElementById("email").value
  const password = document.getElementById("password").value
  const rememberMe = document.getElementById("remember-me").checked

  if (email && password) {
    state.isAuthenticated = true
    state.userEmail = email

    if (rememberMe) {
      localStorage.setItem("userEmail", email)
      localStorage.setItem("rememberMe", "true")
    }

    localStorage.setItem("authToken", "mock-token-" + Date.now())
    showMainApp()
  }
}

function logout() {
  state.isAuthenticated = false
  state.userEmail = ""
  localStorage.removeItem("authToken")
  localStorage.removeItem("rememberMe")
  showAuthScreen()
}

function checkAuth() {
  const token = localStorage.getItem("authToken")
  const email = localStorage.getItem("userEmail")
  const rememberMe = localStorage.getItem("rememberMe")

  if (token && email && rememberMe) {
    state.isAuthenticated = true
    state.userEmail = email
    showMainApp()
  } else {
    showAuthScreen()
  }
}

function showAuthScreen() {
  document.getElementById("auth-screen").classList.add("active")
  document.getElementById("main-app").classList.remove("active")
}

function showMainApp() {
  document.getElementById("auth-screen").classList.remove("active")
  document.getElementById("main-app").classList.add("active")
  document.getElementById("user-email").textContent = state.userEmail
  document.getElementById("profile-email").value = state.userEmail
}

// Navigation
function navigateTo(page) {
  // Hide all pages
  document.querySelectorAll(".page").forEach((p) => {
    p.classList.remove("active")
  })

  // Hide all nav links
  document.querySelectorAll(".nav-link").forEach((link) => {
    link.classList.remove("active")
  })

  // Show selected page
  document.getElementById(page + "-page").classList.add("active")

  // Highlight nav link
  event.target.classList.add("active")

  // Update page title
  const titles = {
    dashboard: "Tableau de bord",
    history: "Historique",
    predictions: "Prédictions IA",
    health: "Conseils Santé",
    profile: "Profil",
  }
  document.getElementById("page-title").textContent = titles[page] || "AirAware"

  state.currentPage = page
  closeSidebar()

  // Initialize charts if needed
  if (page === "dashboard") {
    setTimeout(() => drawTrendChart(), 100)
  } else if (page === "history") {
    setTimeout(() => drawHistoryChart(), 100)
  } else if (page === "predictions") {
    setTimeout(() => drawPredictionChart(), 100)
  }
}

// Data Simulation
function startDataSimulation() {
  // Update metrics every 5 seconds
  setInterval(updateMetrics, 5000)
  updateMetrics() // Initial update
}

function updateMetrics() {
  // Simulate realistic data variations
  state.metrics.aqi = Math.max(0, Math.min(500, state.metrics.aqi + (Math.random() - 0.5) * 10))
  state.metrics.co2 = Math.max(300, Math.min(2000, state.metrics.co2 + (Math.random() - 0.5) * 20))
  state.metrics.pm25 = Math.max(0, state.metrics.pm25 + (Math.random() - 0.5) * 2)
  state.metrics.pm10 = Math.max(0, state.metrics.pm10 + (Math.random() - 0.5) * 3)
  state.metrics.temperature = state.metrics.temperature + (Math.random() - 0.5) * 0.5
  state.metrics.humidity = Math.max(20, Math.min(90, state.metrics.humidity + (Math.random() - 0.5) * 2))

  // Store historical data
  state.historicalData.push({
    timestamp: new Date(),
    ...state.metrics,
  })

  // Keep only last 7 days
  if (state.historicalData.length > 10080) {
    state.historicalData.shift()
  }

  updateDashboardDisplay()
}

function updateDashboardDisplay() {
  const m = state.metrics

  // Update AQI
  document.getElementById("aqi-value").textContent = Math.round(m.aqi)
  const aqiStatus = m.aqi < 50 ? "BON" : m.aqi < 100 ? "MOYEN" : m.aqi < 150 ? "MAUVAIS" : "TRÈS MAUVAIS"
  document.getElementById("aqi-status").textContent = aqiStatus
  document.getElementById("aqi-fill").style.width = (m.aqi / 500) * 100 + "%"

  // Update description
  const descriptions = {
    BON: "Qualité d'air excellente",
    MOYEN: "Qualité d'air acceptable",
    MAUVAIS: "Qualité d'air mauvaise",
    "TRÈS MAUVAIS": "Qualité d'air très mauvaise",
  }
  document.getElementById("aqi-description").textContent = descriptions[aqiStatus]

  // Update metrics
  document.getElementById("metric-co2").textContent = Math.round(m.co2) + " ppm"
  document.getElementById("metric-pm25").textContent = m.pm25.toFixed(1) + " µg/m³"
  document.getElementById("metric-pm10").textContent = m.pm10.toFixed(1) + " µg/m³"
  document.getElementById("metric-temp").textContent = m.temperature.toFixed(1) + " °C"
  document.getElementById("metric-humidity").textContent = Math.round(m.humidity) + "%"
  document.getElementById("metric-pressure").textContent = Math.round(m.pressure) + " hPa"

  // Update status indicators
  updateStatusIndicator(
    "metric-co2",
    m.co2 < 800 ? "Normal" : m.co2 < 1200 ? "Élevé" : "Critique",
    m.co2 < 800 ? "var(--success)" : m.co2 < 1200 ? "var(--warning)" : "var(--error)",
  )
}

function updateStatusIndicator(elementId, text, color) {
  const elem = document.getElementById(elementId + "-status")
  if (elem) {
    elem.textContent = text
    elem.style.color = color
  }
}

// Charts
function drawTrendChart() {
  const canvas = document.getElementById("trend-chart")
  if (!canvas) return

  const ctx = canvas.getContext("2d")
  const width = canvas.parentElement.clientWidth - 48
  canvas.width = width
  canvas.height = 200

  // Generate mock 24h data
  const data = Array(24)
    .fill(0)
    .map(() => Math.random() * 100 + 300)

  drawLineChart(ctx, data, 300, 500, "#558B56", width, 200)
}

function drawHistoryChart() {
  const canvas = document.getElementById("history-chart")
  if (!canvas) return

  const ctx = canvas.getContext("2d")
  const width = canvas.parentElement.clientWidth - 48
  canvas.width = width
  canvas.height = 200

  // Use actual historical data if available
  const data =
    state.historicalData.slice(-24).map((d) => d.co2) ||
    Array(24)
      .fill(0)
      .map(() => Math.random() * 100 + 350)

  drawLineChart(ctx, data, 300, 600, "#558B56", width, 200)
}

function drawPredictionChart() {
  const canvas = document.getElementById("prediction-chart")
  if (!canvas) return

  const ctx = canvas.getContext("2d")
  const width = canvas.parentElement.clientWidth - 48
  canvas.width = width
  canvas.height = 200

  // Generate prediction data (48h ahead)
  const current = state.metrics.co2
  const data = Array(48)
    .fill(0)
    .map((_, i) => current + Math.sin(i / 10) * 50 + (Math.random() - 0.5) * 30)

  drawLineChart(ctx, data, 300, 600, "#4db8a3", width, 200)
}

function drawLineChart(ctx, data, minValue, maxValue, color, width, height) {
  const padding = 40
  const graphWidth = width - padding * 2
  const graphHeight = height - padding * 2

  // Clear
  ctx.clearRect(0, 0, width, height)

  // Draw grid
  ctx.strokeStyle = "#e5e7e4"
  ctx.lineWidth = 1
  for (let i = 0; i <= 5; i++) {
    const y = padding + (graphHeight / 5) * i
    ctx.beginPath()
    ctx.moveTo(padding, y)
    ctx.lineTo(width - padding, y)
    ctx.stroke()
  }

  // Draw data
  ctx.strokeStyle = color
  ctx.lineWidth = 3
  ctx.lineJoin = "round"
  ctx.lineCap = "round"

  ctx.beginPath()
  data.forEach((value, i) => {
    const x = padding + (graphWidth / (data.length - 1)) * i
    const y = padding + graphHeight - ((value - minValue) / (maxValue - minValue)) * graphHeight

    if (i === 0) ctx.moveTo(x, y)
    else ctx.lineTo(x, y)
  })
  ctx.stroke()

  // Draw gradient fill
  const gradient = ctx.createLinearGradient(0, padding, 0, height - padding)
  gradient.addColorStop(0, color + "40")
  gradient.addColorStop(1, color + "00")

  ctx.fillStyle = gradient
  ctx.fill()
}

// Export Data
function exportData(format) {
  if (!state.historicalData.length) {
    alert("Pas de données à exporter")
    return
  }

  if (format === "csv") {
    let csv = "Timestamp,AQI,CO2,PM2.5,PM10,Temperature,Humidity\n"
    state.historicalData.forEach((row) => {
      csv += `${row.timestamp},${Math.round(row.aqi)},${Math.round(row.co2)},${row.pm25.toFixed(1)},${row.pm10.toFixed(1)},${row.temperature.toFixed(1)},${row.humidity.toFixed(0)}\n`
    })
    downloadFile(csv, "airaware-data.csv", "text/csv")
  } else if (format === "pdf") {
    alert("Export PDF - Fonctionnalité à implémenter")
  }
}

function downloadFile(content, filename, type) {
  const blob = new Blob([content], { type })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement("a")
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

// UI Utilities
function toggleSidebar() {
  document.querySelector(".sidebar").classList.toggle("active")
}

function closeSidebar() {
  document.querySelector(".sidebar").classList.remove("active")
}

function toggleAuthMode() {
  alert("Mode inscription - Fonctionnalité à implémenter")
}

function dismissPWA() {
  document.getElementById("pwa-prompt").classList.add("hidden")
}

function saveProfile() {
  const habitat = document.getElementById("habitat-select").value
  const location = document.getElementById("location-input").value

  localStorage.setItem("habitat", habitat)
  localStorage.setItem("sensorLocation", location)

  alert("Profil mis à jour avec succès")
}

function updateHistoryChart() {
  setTimeout(() => drawHistoryChart(), 100)
}

function loadLocalStorage() {
  const habitat = localStorage.getItem("habitat")
  const location = localStorage.getItem("sensorLocation")

  if (habitat) document.getElementById("habitat-select").value = habitat
  if (location) document.getElementById("location-input").value = location
}
