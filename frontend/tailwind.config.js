/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#2563eb',
          dark: '#1d4ed8',
          light: '#dbeafe',
        },
        success: '#16a34a',
        warning: '#f97316',
        error: '#dc2626',
        priority: {
          high: '#dc2626',
          medium: '#f97316',
          low: '#16a34a',
        },
      },
    },
  },
  plugins: [],
}
