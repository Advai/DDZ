/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Custom DDZ theme colors
        primary: {
          DEFAULT: '#10b981', // green-500
          hover: '#059669',   // green-600
          dark: '#047857',    // green-700
        },
        background: {
          DEFAULT: '#111827', // gray-900
          light: '#1f2937',   // gray-800
          lighter: '#374151', // gray-700
        },
        accent: {
          gold: '#f8cf2c',    // Gold for landlord/highlights
          red: '#ef4444',     // Red for hearts/diamonds
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
