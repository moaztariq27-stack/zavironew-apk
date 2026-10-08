package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Deal
import com.example.ui.components.DealItemCard
import com.example.ui.theme.*
import com.example.viewmodel.ZaviroViewModel

@Composable
fun DealsScreen(
    viewModel: ZaviroViewModel,
    onDealClick: (Deal) -> Unit
) {
    val allDeals by viewModel.deals.collectAsState()
    val filteredDeals by viewModel.filteredDeals.collectAsState()
    val selectedDealCategory by viewModel.selectedDealCategory.collectAsState()

    val dealCategories = remember(allDeals) {
        val base = listOf(
            "All",
            "Wraps & Rolls",
            "Pasta Combos",
            "Crispy Chicken",
            "Wings Specials",
            "Family Deals",
            "Special Value"
        )
        val custom = allDeals.filter { it.isAvailable }
            .map { it.category.trim() }
            .filter { it.isNotBlank() && base.none { b -> b.equals(it, ignoreCase = true) } }
            .distinct()
        base + custom
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBlack)
            .testTag("deals_screen")
    ) {
        // Deep Dark / Black Base Promotional Header
        Surface(
            color = ZaviroDarkBase,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ZaviroRed,
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Text(
                            text = "15 AMAZING DEALS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = "VALUE COMBO MENU",
                        color = ZaviroOnDarkPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 0.6.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "MORE FOOD • BIGGER SAVINGS • SERVED PIPING HOT",
                    color = ZaviroOnDarkMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Deal Category Filter Chips on Clean White Surface
        Surface(
            color = ZaviroSurfaceCard,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dealCategories) { cat ->
                    val isSelected = selectedDealCategory.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedDealCategory.value = cat },
                        label = {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ZaviroBlack,
                            labelColor = ZaviroTextSecondary,
                            selectedContainerColor = ZaviroRed,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) ZaviroRed else ZaviroSurfaceBorder,
                            selectedBorderColor = ZaviroRed,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("deal_category_$cat")
                    )
                }
            }
        }

        // Deals List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("deals_list_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filteredDeals, key = { it.id }) { deal ->
                DealItemCard(
                    deal = deal,
                    onOrderDeal = {
                        viewModel.addDealToCart(deal)
                    },
                    onDealClick = onDealClick
                )
            }
        }
    }
}

