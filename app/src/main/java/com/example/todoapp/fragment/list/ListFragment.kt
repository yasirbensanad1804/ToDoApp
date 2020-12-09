package com.example.todoapp.fragment.list

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.todoapp.R
import com.example.todoapp.data.ToDoData
import com.example.todoapp.data.ToDoViewModel
import com.example.todoapp.databinding.FragmentListBinding
import com.example.todoapp.fragment.SharedViewModel
import com.example.todoapp.fragment.list.adapter.ListAdapter
import com.example.todoapp.hideKeyBoard
import com.google.android.material.snackbar.Snackbar
import jp.wasabeef.recyclerview.animators.LandingAnimator
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.android.synthetic.main.fragment_list.view.*


class ListFragment : Fragment(), SearchView.OnQueryTextListener {

    private val mSharedViewModel: SharedViewModel by viewModels()
    private val mTodoViewModel: ToDoViewModel by viewModels()
    private val listAdapter: ListAdapter by lazy { ListAdapter() }

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.mSharedViewModel = mSharedViewModel

        setupRecyclerView()

        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_list, container, false)


        mTodoViewModel.getAllData.observe(viewLifecycleOwner, Observer { data ->
            mSharedViewModel.checkIfDatabaseEmpty(data)
            listAdapter.setData(data)
        })

        view.list_layout.setOnClickListener{
            findNavController().navigate(R.id.action_listFragment_to_updateFragment)
        }
        setHasOptionsMenu(true)

        hideKeyBoard(requireActivity())

        return binding.root

    }

    private fun setupRecyclerView() {
        val rvTodo = binding.rvTodo
        rvTodo.apply {
            layoutManager = StaggeredGridLayoutManager(2, GridLayoutManager.VERTICAL)
            adapter = listAdapter
            itemAnimator = LandingAnimator().apply {
                addDuration = 300
            }
        }
        swipeToDelete(rvTodo)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.list_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_delete_all -> confirmDeleteAllData()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmDeleteAllData() {
        AlertDialog.Builder(requireContext())
                .setTitle("Delete")
                .setMessage("Are you sure wanna delete all your activity?")
                .setPositiveButton("Yes"){ _,_ ->
                    mTodoViewModel.deleteAllData()
                    Toast.makeText(requireContext(), "Successfully deleted all", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .create()
                .show()
    }

    private fun swipeToDelete(recyclerView: RecyclerView){
        val swipeToDeleteCallback = object : SwipeToDelete(){

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val deletedItem = listAdapter.dataList[viewHolder.adapterPosition]
                mTodoViewModel.deleteItem(deletedItem)
                listAdapter.notifyItemRemoved(viewHolder.adapterPosition)
                restoreDeletedData(viewHolder.itemView, deletedItem)
            }

        }
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun restoreDeletedData(view: View, deletedItem: ToDoData){
        val snackbar = Snackbar.make(view, "Deleted: ${deletedItem.title}",Snackbar.LENGTH_LONG)
        snackbar.setAction("Undo"){
            mTodoViewModel.insertData(deletedItem)
        }
        snackbar.show()

    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null){
            searchThroughtDatabase(query)
        }
        return true
    }

    private fun searchThroughtDatabase(query: String) {
        val searchQuery = "%$query%"

        mTodoViewModel.searchDatabase(searchQuery).observe(this, Observer { list->
            list?.let {
                listAdapter.setData(it)
            }
        })

    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (query != null){
            searchThroughtDatabase(query)
        }
        return true
    }

}