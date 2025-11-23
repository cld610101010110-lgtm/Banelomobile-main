package com.project.dba_delatorre_dometita_ramirez_tan

sealed class Routes(val routes: String) {

    object R_DashboardScreen: Routes( "dashboard")
    object R_Login: Routes( "Login")
    object R_InventoryList: Routes( "InventoryListScreen")
    object OrderProcess: Routes("OrderProcessScreen")
    object R_AddProduct: Routes( "AddProductScreen")
    object R_Logo: Routes( "WelcomeLogo")
    object R_InventoryTransfer: Routes( "InventoryTransferScreen")
    object R_WasteMarking: Routes( "WasteMarkingScreen")
    object R_IngredientCostView: Routes( "IngredientCostViewScreen")


}