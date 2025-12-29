import { useNavigate } from 'react-router-dom';
import { Button } from '../components/common/Button';

/**
 * Reusable placeholder page for features not yet implemented.
 * Used for: /rules, /shop, /login (until auth is built)
 *
 * Displays a friendly message with navigation options to go back or return home.
 *
 * @example
 * <Route path="/shop" element={<NotImplementedPage />} />
 */
export function NotImplementedPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background flex items-center justify-center px-4">
      <div className="max-w-md w-full bg-background-light rounded-lg p-8 text-center shadow-2xl border border-gray-700">
        {/* Construction Icon */}
        <div className="text-6xl mb-4 animate-bounce">🚧</div>

        {/* Title */}
        <h1 className="text-3xl font-bold text-white mb-4">Coming Soon</h1>

        {/* Description */}
        <p className="text-gray-400 mb-6 text-lg">
          Sorry! This page is not implemented yet.
        </p>

        {/* Subtitle */}
        <p className="text-gray-500 mb-8 text-sm">
          We're working hard to bring you this feature. Check back soon!
        </p>

        {/* Action Buttons */}
        <div className="space-y-3">
          <Button
            onClick={() => navigate(-1)}
            variant="secondary"
            fullWidth
          >
            ← Go Back
          </Button>
          <Button onClick={() => navigate('/')} variant="primary" fullWidth>
            🏠 Home
          </Button>
        </div>
      </div>
    </div>
  );
}
