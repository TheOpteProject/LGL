//  
//  Copyright (c) 2020 Gevorg Voskanyan, All Rights Reserved.
//  
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License as
//  published by the Free Software Foundation; either version 2 of
//  the License, or (at your option) any later version.
//  
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//  
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
//  MA 02111-1307 USA
//  
//--------------------------------------------------
// A simple thread pool implementation, requiring C++11
//--------------------------------------------------

#ifndef THREAD_POOL_HPP_INCLUDED
#define THREAD_POOL_HPP_INCLUDED

#include <thread>
#include <mutex>
#include <future>
#include <vector>
#include <queue>
#include <condition_variable>
#include <utility>
#include <functional>
#include <cassert>

class thread_pool {
public:
	explicit thread_pool( unsigned num_threads )
		: threads_( num_threads )
	{
		for ( auto &t : threads_ )
			t = std::thread( &thread_pool::thread_function, this );
	}

	// thread_pool is neither copyable nor movable

	~thread_pool()
	{
		{
			std::lock_guard< std::mutex > lock( mutex_ );
			stop_ = true;
			// if there are still tasks in task_queue_, they will get destroyed without having the chance to be executed,
			// resulting in broken_promise exception being stored into them, which is fine
		}
		condvar_.notify_all();
		for ( auto &t : threads_ )
			t.join();
	}

	template < typename F, typename ... Args >
	std::future< void > run( F&& f, Args&&... args )
	{
		std::packaged_task< void () > task( std::bind( std::forward< F >( f ), std::forward< Args >( args )... ) );
		auto fut = task.get_future();
		{
			std::lock_guard< std::mutex > lock( mutex_ );
			task_queue_.push( std::move( task ) );
		}
		condvar_.notify_one();
		return fut;
	}

private:
	std::vector< std::thread > threads_;
	std::mutex mutex_;
	std::condition_variable condvar_;
	// the data members below are protected by mutex_
	std::queue< std::packaged_task< void () > > task_queue_;
	bool stop_ = false;

	void thread_function()
	{
		while ( true ) {
			std::packaged_task< void () > task;
			{
				std::unique_lock< std::mutex > lock( mutex_ );
				condvar_.wait( lock, [this] { return stop_ || !task_queue_.empty(); } );
				if ( stop_ )
					return;
				assert( !task_queue_.empty() );
				task = std::move( task_queue_.front() );
				task_queue_.pop();
			}
			task();	// running the task, outside of the lock
		}
	}
};
  
#endif	// THREAD_POOL_HPP_INCLUDED
