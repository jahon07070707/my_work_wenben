package com.textanalysis.controller;

import com.textanalysis.common.Result;
import com.textanalysis.entity.CollectionTask;
import com.textanalysis.entity.DataSource;
import com.textanalysis.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collection")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping("/sources")
    public Result<List<DataSource>> listSources() {
        return Result.ok(collectionService.listSources());
    }

    @PostMapping("/start/{sourceId}")
    public Result<CollectionTask> start(@PathVariable Long sourceId) {
        return Result.ok(collectionService.startCollection(sourceId));
    }

    @PostMapping("/start-all")
    public Result<CollectionTask> startAll() {
        return Result.ok(collectionService.startAllCollection());
    }

    @GetMapping("/tasks")
    public Result<List<CollectionTask>> recentTasks() {
        return Result.ok(collectionService.recentTasks());
    }
}
