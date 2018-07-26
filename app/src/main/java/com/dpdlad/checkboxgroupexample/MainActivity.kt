package com.dpdlad.checkboxgroupexample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.github.praveen2gemini.CheckBoxGroup

class MainActivity : AppCompatActivity(), CheckBoxGroup.OnCheckedChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val checkboxGroup = findViewById<CheckBoxGroup>(R.id.check_group_id)
        checkboxGroup.setOnCheckedChangeListener(this)
    }

    override fun onCheckedChanged(group: CheckBoxGroup, checkedId: Int, isChecked: Boolean) {
        val allSelectedCheckboxIds = group.checkedCheckboxButtonIds
        Log.e("@@@@@@", "Total Checked CheckBox IDs: $allSelectedCheckboxIds")
        when (checkedId) {
            R.id.check_nexus_id -> printLogInfo("NEXUS ", isChecked)
            R.id.check_iphone_id -> printLogInfo("IPHONE ", isChecked)
            R.id.check_blackberry_id -> printLogInfo("BLACKBERRY ", isChecked)
            R.id.check_htc_id -> printLogInfo("HTC ", isChecked)
            R.id.check_sony_id -> printLogInfo("SONY ", isChecked)
            else -> printLogInfo("All items are ", isChecked)
        }
    }


    private fun printLogInfo(model: String, isChecked: Boolean) {
        Log.e("******", model + ": model " + getState(isChecked))

        Toast.makeText(this
                , "$model " + getState(isChecked), Toast.LENGTH_SHORT).show()
    }

    private fun getState(isChecked: Boolean): String =
            if (isChecked) "CHECKED" else "UN-CHECKED"

}
