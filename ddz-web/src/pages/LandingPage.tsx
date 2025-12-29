import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createGame } from '../services/api';

/**
 * Landing page - ported from MVP design
 */
export function LandingPage() {
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [isCreating, setIsCreating] = useState(false);

  const handleCreateGame = async () => {
    setIsCreating(true);
    setError('');

    try {
      // Create game with 7 player slots (no nickname needed yet)
      const gameInfo = await createGame(7);

      // Store creatorToken in localStorage
      if (gameInfo.creatorToken) {
        localStorage.setItem('creatorToken', gameInfo.creatorToken);
        localStorage.setItem('creatorGameId', gameInfo.sessionId); // Store sessionId
      }

      // Redirect to game page immediately (using sessionId for URL)
      navigate(`/game/${gameInfo.sessionId}`);
    } catch (err) {
      console.error('Failed to create game:', err);
      setError('Failed to create game. Please try again.');
      setIsCreating(false);
    }
  };

  return (
    <div className="min-h-screen p-5">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-center text-5xl mb-8 font-bold" style={{ textShadow: '2px 2px 4px rgba(0,0,0,0.5)' }}>
          🃏 Dou Dizhu (斗地主) 🃏
        </h1>

        <div
          className="p-6 rounded-xl"
          style={{
            background: 'rgba(51, 81, 85, 0.4)',
            backdropFilter: 'blur(10px)',
            border: '1px solid rgba(248, 207, 44, 0.3)',
          }}
        >
          <h2 className="text-2xl font-bold mb-6" style={{ color: '#f8cf2c' }}>
            Set up
          </h2>

          {error && (
            <p className="mb-4 text-sm" style={{ color: '#ab202a' }}>
              {error}
            </p>
          )}

          <button
            onClick={handleCreateGame}
            disabled={isCreating}
            className="btn btn-primary w-full px-6 py-4 text-xl"
          >
            {isCreating ? 'Creating...' : '🎮 Create Game'}
          </button>
        </div>
      </div>
    </div>
  );
}
