import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { TopNav } from './components/layout/TopNav';
import { LandingPage } from './pages/LandingPage';
import { GamePage } from './pages/GamePage';
import { NotImplementedPage } from './pages/NotImplementedPage';

/**
 * Root application component.
 * Sets up routing and layout structure.
 *
 * All routes render TopNav, then page content below.
 */
function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-background">
        <TopNav />
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/game/:sessionId" element={<GamePage />} />
          <Route path="/rules" element={<NotImplementedPage />} />
          <Route path="/shop" element={<NotImplementedPage />} />
          <Route path="/login" element={<NotImplementedPage />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

export default App;
