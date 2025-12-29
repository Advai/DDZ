import { useState, useEffect } from 'react';
import { Modal } from '../common/Modal';

interface SessionStats {
  userId: string;
  username: string;
  displayName: string;
  totalPoints: number;
  landlordWins: number;
  peasantWins: number;
  totalWins: number;
  gamesPlayed: number;
}

interface LeaderboardModalProps {
  isOpen: boolean;
  onClose: () => void;
  gameId: string; // Actually sessionId, but keeping prop name for compatibility
}

export function LeaderboardModal({ isOpen, onClose, gameId: sessionId }: LeaderboardModalProps) {
  const [stats, setStats] = useState<SessionStats[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && sessionId) {
      loadStats();
    }
  }, [isOpen, sessionId]);

  const loadStats = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/games/${sessionId}/session-stats`);
      if (!response.ok) throw new Error('Failed to load stats');
      const data = await response.json();
      setStats(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load leaderboard');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="📊 Session Leaderboard" maxWidth="full">
      <div className="space-y-4">
        {loading && (
          <div className="text-center py-8 text-white text-lg">
            Loading stats...
          </div>
        )}

        {error && (
          <div className="text-center py-4 text-red-400 text-lg">
            {error}
          </div>
        )}

        {!loading && !error && stats.length === 0 && (
          <div className="text-center py-8 text-white text-lg">
            No games completed yet. Finish a round to see stats!
          </div>
        )}

        {!loading && !error && stats.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left" style={{ borderCollapse: 'separate', borderSpacing: '0' }}>
              <thead>
                <tr style={{ background: 'rgba(248, 207, 44, 0.15)' }}>
                  <th className="pb-4 pt-4 px-4 text-yellow-300 font-bold text-lg">Rank</th>
                  <th className="pb-4 pt-4 px-4 text-yellow-300 font-bold text-lg">Player</th>
                  <th className="pb-4 pt-4 px-4 text-yellow-300 font-bold text-lg text-right">Total Points</th>
                  <th className="pb-4 pt-4 px-4 text-yellow-300 font-bold text-lg text-center">👑 Landlord Wins</th>
                  <th className="pb-4 pt-4 px-4 text-yellow-300 font-bold text-lg text-center">🌾 Peasant Wins</th>
                  <th className="pb-4 pt-4 px-4 text-yellow-300 font-bold text-lg text-center">🏆 Total Wins</th>
                </tr>
              </thead>
              <tbody>
                {stats.map((stat, index) => (
                  <tr
                    key={stat.userId}
                    style={{
                      background: index % 2 === 0 ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.2)',
                      borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
                    }}
                  >
                    <td className="py-4 px-4 text-center">
                      {index === 0 && <span className="text-3xl">🥇</span>}
                      {index === 1 && <span className="text-3xl">🥈</span>}
                      {index === 2 && <span className="text-3xl">🥉</span>}
                      {index > 2 && <span className="text-white text-lg font-bold">#{index + 1}</span>}
                    </td>
                    <td className="py-4 px-4 font-bold text-white text-lg">{stat.displayName}</td>
                    <td className={`py-4 px-4 text-right font-bold text-xl ${
                      stat.totalPoints > 0 ? 'text-green-400' :
                      stat.totalPoints < 0 ? 'text-red-400' :
                      'text-gray-400'
                    }`}>
                      {stat.totalPoints > 0 ? '+' : ''}{stat.totalPoints}
                    </td>
                    <td className="py-4 px-4 text-center text-white text-lg font-semibold">{stat.landlordWins}</td>
                    <td className="py-4 px-4 text-center text-white text-lg font-semibold">{stat.peasantWins}</td>
                    <td className="py-4 px-4 text-center font-bold text-yellow-300 text-xl">{stat.totalWins}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </Modal>
  );
}
