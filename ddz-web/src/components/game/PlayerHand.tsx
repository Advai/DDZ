import type { Card } from '../../services/types';

interface PlayerHandProps {
  cards: Card[];
  selectedCards: Set<string>;
  onCardToggle: (cardKey: string) => void;
  canSelect: boolean;
}

const sortCards = (cards: Card[]) => {
  const rankOrder = [
    'THREE',
    'FOUR',
    'FIVE',
    'SIX',
    'SEVEN',
    'EIGHT',
    'NINE',
    'TEN',
    'JACK',
    'QUEEN',
    'KING',
    'ACE',
    'TWO',
    'LITTLE_JOKER',
    'BIG_JOKER',
  ];
  return [...cards].sort((a, b) => rankOrder.indexOf(a.rank) - rankOrder.indexOf(b.rank));
};

const formatRank = (rank: string) => {
  const rankMap: Record<string, string> = {
    THREE: '3',
    FOUR: '4',
    FIVE: '5',
    SIX: '6',
    SEVEN: '7',
    EIGHT: '8',
    NINE: '9',
    TEN: '10',
    JACK: 'J',
    QUEEN: 'Q',
    KING: 'K',
    ACE: 'A',
    TWO: '2',
    LITTLE_JOKER: '🃏',
    BIG_JOKER: '🤡',
  };
  return rankMap[rank] || rank;
};

export function PlayerHand({ cards, selectedCards, onCardToggle, canSelect }: PlayerHandProps) {
  const sortedCards = sortCards(cards);

  // Split into two rows if more than 13 cards
  const shouldSplit = sortedCards.length > 13;
  const row1 = shouldSplit ? sortedCards.slice(0, Math.ceil(sortedCards.length / 2)) : sortedCards;
  const row2 = shouldSplit ? sortedCards.slice(Math.ceil(sortedCards.length / 2)) : [];

  const renderCard = (card: Card, index: number) => {
    const cardKey = `${card.rank}${card.suit[0]}_${index}`;
    const isSelected = selectedCards.has(cardKey);
    const isJoker = card.rank === 'LITTLE_JOKER' || card.rank === 'BIG_JOKER';
    const isRed = card.suit === 'HEARTS' || card.suit === 'DIAMONDS';

    return (
      <div
        key={cardKey}
        className={`card ${isSelected ? 'selected' : ''} ${
          isJoker ? 'joker' : isRed ? 'red' : 'black'
        }`}
        onClick={canSelect ? () => onCardToggle(cardKey) : undefined}
        style={{ cursor: canSelect ? 'pointer' : 'default' }}
      >
        {isJoker ? (
          <div className="rank" style={{ fontSize: '48px' }}>
            {formatRank(card.rank)}
          </div>
        ) : (
          <>
            <div className="rank">{formatRank(card.rank)}</div>
            <div className="suit">
              {card.suit === 'HEARTS'
                ? '♥'
                : card.suit === 'DIAMONDS'
                ? '♦'
                : card.suit === 'CLUBS'
                ? '♣'
                : '♠'}
            </div>
          </>
        )}
      </div>
    );
  };

  return (
    <div className="w-full max-w-5xl mx-auto mb-6">
      <h3 className="text-xl font-bold mb-3" style={{ color: '#f8cf2c' }}>
        Your Hand ({cards.length} cards)
      </h3>
      <div
        className="p-4 rounded-lg"
        style={{ background: 'rgba(21, 20, 26, 0.5)' }}
      >
        {shouldSplit ? (
          <div className="flex flex-col gap-3">
            <div className="flex flex-wrap gap-2 justify-center">
              {row1.map((card, idx) => renderCard(card, idx))}
            </div>
            <div className="flex flex-wrap gap-2 justify-center">
              {row2.map((card, idx) => renderCard(card, row1.length + idx))}
            </div>
          </div>
        ) : (
          <div className="flex flex-wrap gap-2 justify-center">
            {sortedCards.map((card, idx) => renderCard(card, idx))}
          </div>
        )}
      </div>
    </div>
  );
}
