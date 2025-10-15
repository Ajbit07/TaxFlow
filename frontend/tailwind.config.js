/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
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
        sans: ["Seravek", "Gill Sans Nova", "Ubuntu", "Calibri", "DejaVu Sans", "source-sans-pro", "sans-serif"],
        display: ["Rockwell", "Rockwell Nova", "Roboto Slab", "DejaVu Serif", "Sitka Small", "serif"],
      },
    },
  },
  plugins: [],
};
