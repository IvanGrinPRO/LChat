'use client';
import React, { useState } from 'react';

export default function ProfileSettings({ params }: { params: { id: string } }) {
  
  const id = React.use(params).id;
  const [activeTab, setActiveTab] = useState('General');
  
  const [isOnline, setIsOnline] = useState(true);
  const [is2FA, setIs2FA] = useState(true);

  return (
    <div className="min-h-screen bg-[#1a1a1e] flex items-start justify-center p-10 text-white font-sans">
      <div className="w-full max-w-6xl grid grid-cols-[300px_1fr] gap-12">
        
        <aside className="space-y-8">
          <div className="p-8 rounded-[2.5rem] shadow-soft-out flex flex-col items-center">
            <div className="w-28 h-28 rounded-full shadow-soft-out border-[6px] border-[#1e1e22] overflow-hidden mb-4">
              <img src="https://i.pravatar.cc/150?u=2" alt="Avatar" className="w-full h-full object-cover" />
            </div>
            <h2 className="text-xl font-bold tracking-tight text-white">Vanya Dev</h2>
            <span className="text-[10px] text-primary-accent font-black tracking-[0.2em] mt-1 uppercase">Test Plan</span>
          </div>

          <div className="p-4 rounded-[2rem] shadow-soft-in space-y-1">
            {['General', 'Security', 'Billing'].map((item) => (
              <button 
                key={item} 
                onClick={() => setActiveTab(item)}
                className={`w-full text-left px-6 py-4 rounded-xl text-sm font-medium transition-all ${
                  activeTab === item ? 'bg-primary-accent text-white shadow-lg' : 'text-gray-500 hover:text-white'
                }`}
              >
                {item}
              </button>
            ))}
          </div>
        </aside>

        <div className="space-y-8">
          <div className="flex justify-between items-end px-4">
            <div>
              <h1 className="text-4xl font-black tracking-tighter uppercase">{activeTab}</h1>
              <p className="text-gray-500 text-xs font-bold tracking-widest mt-1 uppercase">User ID: {id}</p>
            </div>
            <button className="px-10 py-4 rounded-2xl bg-[#1e1e22] shadow-soft-out text-primary-accent font-bold text-sm hover:shadow-soft-in transition-all">
              DISCARD
            </button>
          </div>

          <div className="p-2 rounded-[3.5rem] shadow-soft-out bg-[#1e1e22]">
            <div className="p-10 space-y-10">
              
              {activeTab === 'General' && (
                <>
                  <section className="grid grid-cols-2 gap-10">
                    <div className="space-y-3">
                      <label className="text-[10px] text-gray-500 font-black tracking-[0.2em] ml-4 uppercase">Username</label>
                      <div className="p-1 rounded-2xl shadow-soft-in">
                        <input type="text" className="w-full bg-transparent p-4 text-sm outline-none" defaultValue="vanya_web" />
                      </div>
                    </div>
                    <div className="space-y-3">
                      <label className="text-[10px] text-gray-500 font-black tracking-[0.2em] ml-4 uppercase">Public Email</label>
                      <div className="p-1 rounded-2xl shadow-soft-in">
                        <input type="email" className="w-full bg-transparent p-4 text-sm outline-none" placeholder="hello@vanya.dev" />
                      </div>
                    </div>
                  </section>
                  <section className="space-y-3">
                    <label className="text-[10px] text-gray-500 font-black tracking-[0.2em] ml-4 uppercase">Bio Description</label>
                    <div className="p-2 rounded-[2rem] shadow-soft-in">
                      <textarea rows={3} className="w-full bg-transparent p-4 text-sm outline-none resize-none" placeholder="I'm a web developer..." />
                    </div>
                  </section>
                </>
              )}

              {activeTab === 'Security' && (
                <>
                  <section className="space-y-6">
                    <div className="space-y-3">
                      <label className="text-[10px] text-gray-500 font-black tracking-[0.2em] ml-4 uppercase">Change Password</label>
                      <div className="p-1 rounded-2xl shadow-soft-in mb-4">
                        <input type="password" placeholder="Current Password" className="w-full bg-transparent p-4 text-sm outline-none" />
                      </div>
                      <div className="p-1 rounded-2xl shadow-soft-in">
                        <input type="password" placeholder="New Password" className="w-full bg-transparent p-4 text-sm outline-none" />
                      </div>
                    </div>
                    
                    <div className="p-8 rounded-3xl shadow-soft-in bg-[#1c1c20]/50 flex items-center justify-between">
                      <div>
                        <h4 className="text-sm font-bold italic">Two-Factor Authentication</h4>
                        <p className="text-[10px] text-gray-500 mt-1">Add an extra layer of security to your account</p>
                      </div>
                      <button 
                        onClick={() => setIs2FA(!is2FA)}
                        className="w-16 h-8 rounded-full shadow-soft-out p-1 flex items-center bg-[#1e1e22] transition-all"
                      >
                        <div className={`w-6 h-6 rounded-full transition-all duration-300 ${
                          is2FA 
                          ? 'bg-green-500 shadow-[0_0_10px_rgba(34,197,94,0.5)] ml-auto' 
                          : 'bg-gray-700 ml-0'
                        }`} />
                      </button>
                    </div>

                    <div className="p-6 rounded-2xl border border-primary-accent/20 bg-primary-accent/5">
                      <h4 className="text-xs font-bold text-primary-accent uppercase tracking-widest mb-2">Sessions</h4>
                      <p className="text-[10px] text-gray-400">You are currently logged in on 2 devices. <span className="text-primary-accent underline cursor-pointer ml-1">Log out from all</span></p>
                    </div>
                  </section>
                </>
              )}

              {activeTab === 'Billing' && (
                <>
                  <section className="space-y-8">
                    <div className="grid grid-cols-2 gap-6">
                      <div className="p-8 rounded-[2rem] shadow-soft-in bg-gradient-to-br from-[#222226] to-[#1a1a1e]">
                        <p className="text-[10px] text-gray-500 font-bold uppercase tracking-widest">Active Plan</p>
                        <h3 className="text-2xl font-black mt-2 text-primary-accent italic tracking-tighter">TEST PLAN</h3>
                        <p className="text-[10px] text-gray-400 mt-4">$0.00 / month</p>
                      </div>
                      <div className="p-8 rounded-[2rem] shadow-soft-out flex flex-col justify-center border border-white/5">
                        <p className="text-[10px] text-gray-500 font-bold uppercase">Next Payment</p>
                        <h3 className="text-xl font-bold mt-1 text-white">May 20, 2026</h3>
                      </div>
                    </div>

                    <div className="space-y-3">
                      <label className="text-[10px] text-gray-500 font-black tracking-[0.2em] ml-4 uppercase">Payment Method</label>
                      <div className="p-6 rounded-2xl shadow-soft-in flex items-center justify-between">
                        <div className="flex items-center gap-4">
                          <div className="w-12 h-8 bg-[#252529] rounded shadow-inner flex items-center justify-center text-[8px] font-bold">VISA</div>
                          <span className="text-sm font-medium">•••• •••• •••• 4242</span>
                        </div>
                        <button className="text-[10px] font-black text-primary-accent hover:underline">UPDATE</button>
                      </div>
                    </div>
                  </section>
                </>
              )}

              <section className="flex items-center justify-between p-8 rounded-3xl shadow-soft-in bg-[#1c1c20]/50">
                <div>
                  <h4 className="text-sm font-bold">Show Online Status</h4>
                  <p className="text-[10px] text-gray-500 mt-1">Others will see when you are active</p>
                </div>
                <button 
                  onClick={() => setIsOnline(!isOnline)}
                  className="w-16 h-8 rounded-full shadow-soft-out p-1 flex items-center transition-all bg-[#1e1e22]"
                >
                  <div className={`w-6 h-6 rounded-full transition-all duration-300 ${
                    isOnline 
                    ? 'bg-primary-accent shadow-md shadow-primary-accent/40 ml-auto' 
                    : 'bg-gray-700 ml-0'
                  }`} />
                </button>
              </section>

              <footer className="pt-4 flex gap-4">
                <button className="flex-1 bg-primary-accent py-5 rounded-[2rem] font-bold text-sm shadow-lg shadow-primary-accent/20 hover:brightness-110 active:scale-[0.98] transition-all tracking-widest">
                  SAVE {activeTab.toUpperCase()}
                </button>
              </footer>

            </div>
          </div>
        </div>

      </div>
    </div>
  );
}