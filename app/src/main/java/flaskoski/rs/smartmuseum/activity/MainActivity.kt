package flaskoski.rs.smartmuseum.activity

import androidx.lifecycle.ViewModelProvider
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.snackbar.Snackbar
import flaskoski.rs.smartmuseum.listAdapter.ItemsGridListAdapter
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_main_bottom_sheet.*
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.databinding.ActivityMainBinding
import flaskoski.rs.smartmuseum.model.GroupItem
import flaskoski.rs.smartmuseum.model.ItemRepository
import flaskoski.rs.smartmuseum.util.NetworkVerifier
import flaskoski.rs.smartmuseum.viewmodel.JourneyManager
import kotlinx.android.synthetic.main.activity_feature_preferences.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.next_item.*
import kotlinx.android.synthetic.main.next_item.view.*
import java.lang.IllegalStateException


class MainActivity : AppCompatActivity(), ItemsGridListAdapter.OnShareClickListener {

    private val requestGetPreferences: Int = 1
    private val requestItemRatingChange: Int = 2
    private val requestQuestionnaire: Int = 4
    var isFirstItem: Boolean = true

    private val TAG = "MainActivity"
//    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
//        when (item.itemId) {
//            R.id.navigation_home -> {
//                return@OnNavigationItemSelectedListener true
//            }
//            R.id.navigation_dashboard -> {
//                return@OnNavigationItemSelectedListener true
//            }
//            R.id.navigation_notifications -> {
//              //  message.setText(R.string.title_notifications)
//                return@OnNavigationItemSelectedListener true
//            }
//        }
//        false
//    }

    private lateinit var adapter: ItemsGridListAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var journeyManager : JourneyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        //------------Standard Side Menu Screen---------------------------
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityMainBinding>(
                this, R.layout.activity_main)

        //draw toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FF0099CC")))

        loading_view.visibility = View.VISIBLE

        //attach view model to activity
        journeyManager = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(JourneyManager::class.java)

        //journeyManager activity, userLocation and maps setup
        journeyManager.updateActivity(this)
        journeyManager.buildMap(supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)

        //set observable states
        journeyManager.isPreferencesSet.observe(this, preferencesSetListener)
        journeyManager.isItemsAndRatingsLoaded.observe(this, isItemsAndRatingsLoadedListener  )
        journeyManager.isJourneyBegan.observe(this, isJourneyBeganListener)
        journeyManager.isCloseToItem.observe(this, closeToItemIsChangedListener)
        journeyManager.isGoToNextItem.observe(this, isGoToNextItemListener)
        journeyManager.isJourneyFinishedFlag.observe(this, isJourneyFinishedListener)
        journeyManager.itemListChangedListener = {
            @Suppress("UNNECESSARY_SAFE_CALL")
            adapter?.notifyDataSetChanged()
            if(view_next_item.visibility == View.VISIBLE)
                updateNextItemCard()
            loading_view.visibility = View.GONE
        }

        //bottomsheet setup and bring views to front
        bottomSheetBehavior = BottomSheetBehavior.from(sheet_next_items)
        ApplicationProperties.bringToFront(loading_view, 50f)
        ApplicationProperties.bringToFront(sheet_next_items, 40f)
        ApplicationProperties.bringToFront(view_next_item, 30f)

   //     navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        //GridItems setup
        itemsGridList.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemsGridListAdapter(journeyManager.itemsList, applicationContext, this, ItemRepository.recommenderManager)
        itemsGridList.adapter = adapter

        if(journeyManager.isItemsAndRatingsLoaded.value!! && journeyManager.isJourneyBegan.value!!)
        {
            journeyManager.setNextRecommendedDestination()
        }else if(journeyManager.recoverSavedPreferences() == null){
            //--DEBUG
//                @Suppress("ConstantConditionIf")
//                if(isDebugging) {
//                    ApplicationProperties.user = User("Felipe", "Felipe", 155.0)
//                    bt_begin_route.visibility = View.VISIBLE
//                    journeyManager.isPreferencesSet.value = true
//                }
//                //--DEBUG
//                else {
            val getPreferencesIntent = Intent(applicationContext, FeaturePreferencesActivity::class.java)
            startActivityForResult(getPreferencesIntent, requestGetPreferences)
//                }
        }

        ApplicationProperties.checkForUpdates(ApplicationProperties.getCurrentVersionCode(applicationContext)){isThereUpdates ->
            if(isThereUpdates)
                if(ApplicationProperties.checkIfForceUpdateIsOn() == true)
                    AlertBuider().showUpdateRequired(this@MainActivity){
                        finish()
                    }
                else{
                    AlertBuider().showUpdateAvailable(this@MainActivity)
                }
        }
        if(!NetworkVerifier().isNetworkAvailable(applicationContext))
            AlertBuider().showNetworkDisconnected(this@MainActivity)
    }

    //Show next item card on screen
    private val closeToItemIsChangedListener = Observer<Boolean> { isClose : Boolean ->
        if(isClose && journeyManager.showNextItem_okPressed) {
            journeyManager.showNextItem_okPressed = false
            view_next_item.lb_info.text = getString(R.string.lb_you_arrived)
            view_next_item.visibility = View.VISIBLE
            bt_ok.visibility = View.GONE
            view_next_item.setOnClickListener { this.shareOnItemClicked(0, true) }
        }
    }

    private val preferencesSetListener = Observer<Boolean>{ preferencesSet : Boolean ->
        if(preferencesSet && !journeyManager.isJourneyBegan.value!!){
            showStartMessage()
        }
    }

    override fun onDestroy() {
        journeyManager.setOnDestroyActivityState()
        super.onDestroy()
    }

    private val isItemsAndRatingsLoadedListener = Observer<Boolean>{ loaded : Boolean ->
        if(loaded && journeyManager.isJourneyBegan.value!!) {
            journeyManager.recoverCurrentState()
            loading_view.visibility = View.GONE
            //DEBUG
            //shareOnItemClicked(journeyManager.itemsList.indexOf(journeyManager.itemsList.find{ it.id == "7I7lVxSXOjvYWE2e5i72"}),false)
        }
    }

    private val isJourneyBeganListener = Observer<Boolean> { isJourneyBegan: Boolean ->
    }

    private val isJourneyFinishedListener = Observer<Boolean> { isJourneyFinished: Boolean ->
        if(isJourneyFinished){
            AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
                    .setTitle("Atenção")
                    .setIcon(R.drawable.baseline_done_black_24)
                    .setMessage("""Você já visitou todas as atrações recomendadas para você dentro do seu tempo disponível.
                        |Por favor nos informe agora o que achou da visita com essa rápida pesquisa.""".trimMargin())
                    .setNeutralButton(R.string.ok){_,_ ->
                        val goToQuestionnaire = Intent(applicationContext, QuestionnaireActivity::class.java)
                        startActivityForResult(goToQuestionnaire, requestQuestionnaire)}
                    .show()

        }
    }

    private fun showStartMessage() {
        val startDialog = AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
        startDialog.setTitle(getString(R.string.welcome_title))
                .setMessage(getString(R.string.welcome_message))
                .setNeutralButton(android.R.string.ok) { _, _ -> }
                .setOnDismissListener { beginJourney() }
        startDialog.show()
    }

    private fun showNextItemCard(){
        if(journeyManager.isRatingChanged) {
            lb_info.text = getString(R.string.lb_next_item_with_rating_change)
            journeyManager.nextItemCardShowedWithRatingChangeWarning()
        }else lb_info.text = getString(R.string.lb_next_item)

        bt_ok.visibility = View.VISIBLE

        view_next_item.lb_next_item_name.text = journeyManager.itemsList[0].title
        view_next_item.next_item_ratingBar.rating = journeyManager.itemsList[0].recommedationRating
        ItemRepository.loadImage(applicationContext, view_next_item.next_item_img_itemThumb, journeyManager.itemsList[0].photoId)
        view_next_item.visibility = View.VISIBLE
        view_next_item.setOnClickListener{}
    }
    private val isGoToNextItemListener = Observer<Boolean> { isCurrentItemVisited: Boolean ->
        if(isCurrentItemVisited && journeyManager.isJourneyBegan.value!!){
            showNextItemCard()
        }
    }


    private fun updateNextItemCard() {
        view_next_item.lb_next_item_name.text = journeyManager.itemsList[0].title
        view_next_item.next_item_ratingBar.rating = journeyManager.itemsList[0].recommedationRating
        ItemRepository.loadImage(applicationContext, view_next_item.next_item_img_itemThumb, journeyManager.itemsList[0].photoId)
    }

    fun onClickNextItemOk(v : View){
        journeyManager.showNextItem_okPressed = true
        view_next_item.visibility = View.GONE
        if(isFirstItem) {
            Toast.makeText(applicationContext, getString(R.string.follow_line),
                    Toast.LENGTH_LONG).show()
            isFirstItem = false
        }
        closeToItemIsChangedListener.onChanged(journeyManager.isCloseToItem.value)
    }

    fun beginJourney(){
        try {
            journeyManager.beginJourney()
        }
        catch (e: IllegalStateException){
            Log.e(TAG, e.message)
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(!NetworkVerifier().isNetworkAvailable(applicationContext))
            AlertBuider().showNetworkDisconnected(this@MainActivity)

        if (resultCode == RESULT_OK && data != null) {
            loading_view.visibility = View.VISIBLE

            when (requestCode) {
                journeyManager.REQUEST_CHANGE_LOCATION_SETTINGS -> {
                    journeyManager.changeLocationSettingsResult()
                }
                requestGetPreferences ->{
                    journeyManager.getPreferencesResult(data)
                }
                requestItemRatingChange-> {
                    journeyManager.itemRatingChangeResult(data)
                }
                requestQuestionnaire->{
                    if(journeyManager.isJourneyFinishedFlag.value!!) {
                        AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
                                .setTitle("Atenção")
                                .setIcon(R.drawable.baseline_done_black_24)
                                .setMessage("""Você já visitou todas as atrações recomendadas para você dentro do seu tempo disponível. Obrigado pela visita!
                        |Deseja continuar a usar o aplicativo para ver detalhes de mais itens?""".trimMargin())
                                .setPositiveButton(android.R.string.yes) { _, _ -> }
                                .setNegativeButton(R.string.no) { _, _ ->
                                    val getPreferencesIntent = Intent(applicationContext, FeaturePreferencesActivity::class.java)
                                    startActivityForResult(getPreferencesIntent, requestGetPreferences)
                                }
                                .show()
                        view_next_item.visibility = View.GONE
                    }
                }
            }
            loading_view.visibility = View.GONE
        }
    }

    //-----------onClick --------------

    override fun shareOnItemClicked(p1: Int, isArrived : Boolean) {
        if(ApplicationProperties.user == null) {
            Toast.makeText(applicationContext, "Usário não definido! Primeiro informe seu nome na página de preferências.", Toast.LENGTH_LONG).show()
            return
        }
        var viewItemDetails : Intent
        //var subItems : ArrayList<Itemizable>? = null
        if(journeyManager.itemsList[p1] is GroupItem) {
            viewItemDetails = Intent(applicationContext, GroupItemDetailActivity::class.java)
//            subItems = journeyManager.getSubItemsOf(journeyManager.itemsList[p1] as GroupItem) as ArrayList<Itemizable>
        }
        else viewItemDetails = Intent(applicationContext, ItemDetailActivity::class.java)
        val itemId = journeyManager.itemsList[p1].id
        var itemRating : Float = 0F
        journeyManager.ratingsList.find { it.user == ApplicationProperties.user?.id
                && it.item == itemId }?.let {
            itemRating = it.rating
        }

        viewItemDetails.putExtra("itemClicked",  journeyManager.itemsList[p1])
        //viewItemDetails.putExtra("subItems",  subItems)
        viewItemDetails.putExtra("itemRating",  itemRating)

        ApplicationProperties.user?.let {
            it.location = journeyManager.userLocationManager?.userLatLng
        }?: Log.e(TAG, "gridItem(${p1})OnClick - user not defined!")

        if(p1 == 0 && journeyManager.isCloseToItem.value!!)
            viewItemDetails.putExtra(ApplicationProperties.TAG_ARRIVED, true)
        else viewItemDetails.putExtra(ApplicationProperties.TAG_ARRIVED, isArrived)
        startActivityForResult(viewItemDetails, requestItemRatingChange)
    }

    override fun shareOnRemoveItemClicked(p1: Int) {
        val confirmationDialog = AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
        confirmationDialog.setTitle("Atenção")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("Tem certeza que deseja remover essa atração da sua rota recomendada?")
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    journeyManager?.removeItemFromRoute(journeyManager.itemsList[p1]){
                        Snackbar.make(sheet_next_items, getString(R.string.item_removed), Snackbar.LENGTH_SHORT).show()
                    }
                   // view_next_item.visibility = View.GONE
                }.setNegativeButton(android.R.string.no){ _, _ -> }
        confirmationDialog.show()
    }


    //-------------MAPS AND LOCATION----------------------------------------

    override fun onResume() {
        super.onResume()
        journeyManager.userLocationManager?.startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        journeyManager.userLocationManager?.stopLocationUpdates()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                journeyManager.userLocationManager?.createLocationRequest()
            }

        }
    }

    fun goToUserLocation(@Suppress("UNUSED_PARAMETER") v: View) {
        journeyManager.focusOnUserPosition()
    }


    //------------LAYOUT FEATURES-------------------------------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.option_features -> {
                val goToFeaturePreferences = Intent(applicationContext, FeaturePreferencesActivity::class.java)
                // goToPlayerProfileIntent.putExtra("uid", uid)
                startActivityForResult(goToFeaturePreferences, requestGetPreferences)
                true
            }
            R.id.option_questionnaire->{
                val goToQuestionnaire = Intent(applicationContext, QuestionnaireActivity::class.java)
                startActivityForResult(goToQuestionnaire, requestQuestionnaire)
                true
            }
            R.id.option_restart -> {
                val confirmationDialog = AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
                confirmationDialog.setTitle("Atenção")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage("Deseja recomeçar a sua visita? Isso irá apagar suas informações de itens que já visitou.")
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            journeyManager.restartJourney()
                            view_next_item.visibility = View.GONE
                            showStartMessage()
                        }.setNegativeButton(android.R.string.no){ _, _ -> }
                confirmationDialog.show()
                true
            }
            R.id.option_finish -> {
                journeyManager.finishJourney()
                true
            }
            R.id.option_debug->{
                ApplicationProperties.isDebugOn = !ApplicationProperties.isDebugOn
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun toggleSheet(@Suppress("UNUSED_PARAMETER") v: View){
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
         else
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }
}



