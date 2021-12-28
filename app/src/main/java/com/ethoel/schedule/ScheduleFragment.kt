package com.ethoel.schedule

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment

class ScheduleFragment(var counter: Int) : Fragment() {
    private var textView: TextView? = null
    val data1: String =
        """If you really want to hear about it, the first thing you’ll probably want to know is"""
    val data2: String =
        """where I was born, and what my lousy childhood was like,
        and how my parents were occupied
        and all before they had me, and all that David Copperfield kind of crap, but I don’t feel
        like going into it, if you want to know the truth."""
    val data3: String = """In the first place, that stuff bores me,
        and in the second place, my parents would have two hemorrhages apiece if I told anything
        pretty personal about them. They’re quite touchy about anything like that, especially my
        father. They’re nice and all - I’m not saying that - but they’re also touchy as hell.
        Besides, I’m not going to tell you my whole goddamn autobiography or anything. I’ll just
        tell you about this madman stuff that happened to me last Christmas just before I got pretty
        run-down and had to come out and take it easy."""
    val data4: String = """I mean that’s all I told D.B. about, and he’s
        my brother and all. He’s in Hollywood. That isn’t too far from this crumby place, and he
        comes over and visits me practically every week end. He’s going to drive me home when I go
        home next month maybe. He just got a Jaguar. One of those little English jobs that can do
        around two hundred miles an hour. It cost him damn near four thousand bucks."""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        val scheduleView : NestedScrollView = inflater.inflate(R.layout.schedule_view, container, false) as NestedScrollView
        with(scheduleView.getChildAt(0) as TextView) {
            text = tempData(counter)
            textView = this
        }
        return scheduleView
    }

    fun updateView(counter: Int) {
        this.counter = counter
        textView?.text = tempData(counter)
    }

    private fun tempData(number: Int): String {
        return "$data1    $number$number$number$number    $data2$data3$data4"
    }
}