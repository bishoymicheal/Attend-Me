package net.corpy.loginlocation.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp


data class Attendance(
    @ServerTimestamp var inTime: Timestamp?,
    @ServerTimestamp var onTime: Timestamp?
)