import React from 'react';

export default function ChatsPage() {
  const conversations = [
    { id: 1, name: 'Kathy Gomez', msg: 'Typing...', time: '4:09 PM', active: true, online: true },
    { id: 2, name: 'Sara Sanders', msg: 'Can you buy me dinner?', time: '12:35', count: 100 },
    { id: 3, name: 'Doris Diaz', msg: 'Read this article, it is so awesome...', time: '12:35', count: '99+' },
  ];

  return (
    <div className="h-screen bg-[#1a1a1e] p-6 flex gap-8 text-white overflow-hidden">
      
      <aside className="w-[380px] flex flex-col gap-6">
        <header className="flex justify-between items-center px-4">
          <h1 className="text-3xl font-black tracking-tighter italic">CHATS</h1>
          <button className="w-12 h-12 rounded-full shadow-soft-out flex items-center justify-center text-primary-accent text-xl font-bold hover:shadow-soft-in transition-all">
            +
          </button>
        </header>

        <div className="px-2">
          <div className="p-1 rounded-2xl shadow-soft-in">
            <input 
              type="text" 
              placeholder="Search" 
              className="w-full bg-transparent p-4 text-sm outline-none placeholder:text-gray-600"
            />
          </div>
        </div>

        <div className="flex-grow overflow-y-auto px-2 space-y-4 custom-scrollbar">
          {conversations.map((chat) => (
            <div 
              key={chat.id} 
              className={`p-4 rounded-[2rem] cursor-pointer transition-all flex items-center gap-4 ${
                chat.active ? 'shadow-soft-in bg-[#1c1c20]/50' : 'shadow-soft-out hover:bg-[#1e1e22]'
              }`}
            >
              <div className="relative">
                <div className="w-14 h-14 rounded-full shadow-soft-out border-2 border-[#1e1e22] overflow-hidden">
                  <img src={`https://i.pravatar.cc/150?u=${chat.id}`} alt="avatar" />
                </div>
                {chat.online && <div className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-[#1a1a1e] shadow-[0_0_8px_#22c55e]" />}
              </div>
              <div className="flex-grow min-w-0">
                <div className="flex justify-between items-center mb-1">
                  <h3 className="font-bold text-sm truncate">{chat.name}</h3>
                  <span className="text-[10px] text-gray-500 font-bold">{chat.time}</span>
                </div>
                <p className={`text-xs truncate ${chat.active ? 'text-primary-accent font-bold' : 'text-gray-500'}`}>
                  {chat.msg}
                </p>
              </div>
              {chat.count && (
                <div className="bg-primary-accent px-2 py-1 rounded-full text-[10px] font-black shadow-lg shadow-primary-accent/30">
                  {chat.count}
                </div>
              )}
            </div>
          ))}
        </div>
      </aside>

      <main className="flex-grow flex flex-col rounded-[3.5rem] shadow-soft-out bg-[#1e1e22] overflow-hidden border border-white/[0.02]">
        
        <header className="p-8 flex justify-between items-center border-b border-white/[0.03]">
          <div className="flex items-center gap-4">
            <button className="w-10 h-10 rounded-full shadow-soft-out flex items-center justify-center text-gray-500 hover:text-white mr-2">
              ←
            </button>
            <div className="w-12 h-12 rounded-full shadow-soft-out border-2 border-[#1e1e22] overflow-hidden">
              <img src="https://i.pravatar.cc/150?u=1" alt="avatar" />
            </div>
            <div>
              <h2 className="font-bold text-sm">Kathy Gomez</h2>
              <p className="text-[10px] text-primary-accent font-black tracking-widest uppercase">Typing...</p>
            </div>
          </div>
          <div className="flex gap-4">
            <button className="w-12 h-12 rounded-full shadow-soft-out flex items-center justify-center hover:shadow-soft-in transition-all">📞</button>
            <button className="w-12 h-12 rounded-full shadow-soft-out flex items-center justify-center hover:shadow-soft-in transition-all text-primary-accent">● ● ●</button>
          </div>
        </header>

        <div className="flex-grow p-10 overflow-y-auto space-y-8 custom-scrollbar bg-[#1c1c20]/30">
          <div className="text-center">
            <span className="text-[10px] text-gray-600 font-black tracking-[0.3em] uppercase">Today</span>
          </div>


          <div className="flex flex-col items-start gap-2 max-w-[70%]">
            <div className="p-5 rounded-[2rem] rounded-tl-none shadow-soft-out bg-[#1e1e22] text-sm leading-relaxed border border-white/5">
              Hello, How are you?
            </div>
            <span className="text-[9px] text-gray-600 ml-2 font-bold uppercase">4:09 PM</span>
          </div>


          <div className="flex flex-col items-end gap-2 ml-auto max-w-[70%]">
            <div className="p-5 rounded-[2rem] rounded-tr-none bg-primary-accent text-white text-sm font-medium shadow-lg shadow-primary-accent/20 border border-white/10 leading-relaxed">
              I wonder if you would like to watch movie tonight?
            </div>
            <span className="text-[9px] text-primary-accent mr-2 font-bold uppercase">4:09 PM</span>
          </div>

          <div className="flex flex-col items-start gap-2 max-w-[70%]">
            <div className="p-5 rounded-[2rem] rounded-tl-none shadow-soft-out bg-[#1e1e22] text-sm leading-relaxed border border-white/5">
              Sounds like a good idea! 🎬
            </div>
          </div>
        </div>

        <footer className="p-8">
          <div className="p-2 rounded-[2.5rem] shadow-soft-in flex items-center gap-4 bg-[#1a1a1e]/50">
            <button className="w-12 h-12 flex items-center justify-center text-xl grayscale hover:grayscale-0 transition-all">😊</button>
            <input 
              type="text" 
              placeholder="Type a message..." 
              className="flex-grow bg-transparent text-sm outline-none py-4"
            />
            <button className="w-12 h-12 rounded-full shadow-soft-out flex items-center justify-center hover:shadow-soft-in transition-all">📎</button>
            <button className="w-14 h-14 rounded-[1.5rem] bg-primary-accent shadow-lg shadow-primary-accent/20 flex items-center justify-center hover:brightness-110 active:scale-90 transition-all">
              <span className="text-white text-xl">📷</span>
            </button>
          </div>
        </footer>
      </main>
    </div>
  );
}