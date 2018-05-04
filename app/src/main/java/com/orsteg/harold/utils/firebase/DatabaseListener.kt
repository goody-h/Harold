package com.orsteg.harold.utils.firebase

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

/**
 * Created by goodhope on 4/14/18.
 */
interface ValueListener: ValueEventListener{
    override fun onCancelled(p0: DatabaseError?) {

    }

    override fun onDataChange(dataSnapshot: DataSnapshot?) {

    }

}

interface ChildListener: ChildEventListener{
    override fun onCancelled(p0: DatabaseError?) {

    }

    override fun onChildMoved(dataSnapshot: DataSnapshot?, s: String?) {

    }

    override fun onChildChanged(dataSnapshot: DataSnapshot?, s: String?) {

    }

    override fun onChildAdded(dataSnapshot: DataSnapshot?, s: String?) {

    }

    override fun onChildRemoved(dataSnapshot: DataSnapshot?) {

    }

}