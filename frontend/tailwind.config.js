/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#effaf8",
          100: "#d7f0ec",
          200: "#b0e0d9",
          300: "#7fc8bf",
          400: "#4fa9a0",
          500: "#348b84",
          600: "#28706b",
          700: "#235a57",
          800: "#1f4846",
          900: "#1c3b3a",
        },
      },
      fontFamily: {
        sans: ["-apple-system", "BlinkMacSystemFont", "Segoe UI", "Roboto", "Helvetica Neue", "Arial", "sans-serif"],
        display: ["Georgia", "Cambria", "Times New Roman", "serif"],
      },
      keyframes: {
        "page-in": {
          "0%": { opacity: "0", transform: "translateY(8px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        "pop-in": {
          "0%": { opacity: "0", transform: "scale(0.97)" },
          "100%": { opacity: "1", transform: "scale(1)" },
        },
      },
      animation: {
        "page-in": "page-in 0.35s cubic-bezier(0.22, 1, 0.36, 1) both",
        "pop-in": "pop-in 0.2s ease-out both",
      },
    },
  },
  plugins: [],
};
